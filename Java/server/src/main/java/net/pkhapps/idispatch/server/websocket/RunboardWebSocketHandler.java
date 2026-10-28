package net.pkhapps.idispatch.server.websocket;

import net.pkhapps.idispatch.server.control.RunboardDispatcher;
import net.pkhapps.idispatch.server.entity.DispatchNotification;
import net.pkhapps.idispatch.server.entity.Resource;
import net.pkhapps.idispatch.server.events.RunboardNotificationQueued;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket endpoint for runboards. A runboard connects with its runboard key as the {@code runboardKey} query
 * parameter, receives every dispatch notification waiting for it as a {@code notifications} message, and acknowledges
 * each one with an {@code ack} message. Waiting notifications are sent when the runboard connects and whenever a new
 * one is queued for it.
 * <p>
 * The server pings every runboard at the interval set by {@code idispatch.runboard.heartbeat-interval} and closes
 * connections that have not been heard from in three intervals, so that dead connections are detected and idle ones
 * are kept alive.
 * <p>
 * Every notification carries the language of the installation, set by {@code idispatch.runboard.language} as an
 * ISO 639-1 code such as {@code fi} or {@code sv}. Runboards use it to pick the voice that reads the notification out
 * loud, so it should match the language the assignment types and municipalities have been entered in.
 */
@Component
class RunboardWebSocketHandler extends TextWebSocketHandler implements ApplicationListener<RunboardNotificationQueued> {

    static final String RUNBOARD_KEY_PARAMETER = "runboardKey";
    private static final int SEND_TIME_LIMIT_MS = 5000;
    private static final int SEND_BUFFER_SIZE_LIMIT = 512 * 1024;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RunboardDispatcher runboardDispatcher;
    private final JsonMapper jsonMapper;
    private final Duration heartbeatTimeout;
    private final String language;
    private final Map<String, RunboardConnection> connections = new ConcurrentHashMap<>();

    RunboardWebSocketHandler(RunboardDispatcher runboardDispatcher, JsonMapper jsonMapper,
                             @Value("${idispatch.runboard.heartbeat-interval:10s}") Duration heartbeatInterval,
                             @Value("${idispatch.runboard.language:fi}") String language) {
        this.runboardDispatcher = runboardDispatcher;
        this.jsonMapper = jsonMapper;
        this.heartbeatTimeout = heartbeatInterval.multipliedBy(3);
        this.language = language;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        final String runboardKey = Optional.ofNullable(session.getUri())
                .map(uri -> UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst(RUNBOARD_KEY_PARAMETER))
                .filter(key -> !key.isBlank())
                .orElse(null);
        if (runboardKey == null) {
            logger.warn("Rejecting runboard connection {} without a runboard key", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing runboard key"));
            return;
        }
        logger.info("Runboard {} connected from {}", runboardKey, session.getRemoteAddress());
        final RunboardConnection connection = new RunboardConnection(runboardKey,
                new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_LIMIT));
        connections.put(session.getId(), connection);
        sendWaitingNotifications(connection);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        final RunboardConnection connection = connections.get(session.getId());
        if (connection == null) {
            return;
        }
        connection.touch();
        final JsonNode json;
        try {
            json = jsonMapper.readTree(message.getPayload());
        } catch (JacksonException ex) {
            logger.warn("Ignoring malformed message from runboard {}: {}", connection.runboardKey, message.getPayload());
            return;
        }
        if ("ack".equals(json.path("type").asString()) && json.path("id").isIntegralNumber()) {
            runboardDispatcher.acknowledgeDispatchNotification(connection.runboardKey, json.path("id").asLong());
        } else {
            logger.warn("Ignoring unknown message from runboard {}: {}", connection.runboardKey, message.getPayload());
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        Optional.ofNullable(connections.get(session.getId())).ifPresent(RunboardConnection::touch);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.debug("Transport error on runboard connection {}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        final RunboardConnection connection = connections.remove(session.getId());
        if (connection != null) {
            logger.info("Runboard {} disconnected: {}", connection.runboardKey, status);
        }
    }

    @Override
    public void onApplicationEvent(RunboardNotificationQueued event) {
        connections.values().stream()
                .filter(connection -> connection.runboardKey.equals(event.getRunboardKey()))
                .forEach(this::sendWaitingNotifications);
    }

    private void sendWaitingNotifications(RunboardConnection connection) {
        final Collection<DispatchNotification> notifications = runboardDispatcher.getDispatchNotifications(connection.runboardKey);
        if (notifications.isEmpty()) {
            return;
        }
        logger.debug("Sending notifications {} to runboard {}", notifications, connection.runboardKey);
        final Map<String, Object> message = new HashMap<>();
        message.put("type", "notifications");
        message.put("notifications", notifications.stream().map(this::notificationToMap).toList());
        send(connection, new TextMessage(jsonMapper.writeValueAsString(message)));
    }

    @Scheduled(fixedDelayString = "${idispatch.runboard.heartbeat-interval:10s}")
    void heartbeat() {
        final Instant deadline = Instant.now().minus(heartbeatTimeout);
        connections.values().forEach(connection -> {
            if (connection.lastSeen.isBefore(deadline)) {
                logger.warn("Runboard {} has not responded since {}, closing connection", connection.runboardKey, connection.lastSeen);
                close(connection, CloseStatus.SESSION_NOT_RELIABLE);
            } else {
                send(connection, new PingMessage(ByteBuffer.allocate(0)));
            }
        });
    }

    private void send(RunboardConnection connection, WebSocketMessage<?> message) {
        // Never throw: this runs on the scheduler thread and on the async dispatcher thread that published the event.
        try {
            connection.session.sendMessage(message);
        } catch (Exception ex) {
            logger.warn("Could not send message to runboard {}, closing connection", connection.runboardKey, ex);
            close(connection, CloseStatus.SERVER_ERROR);
        }
    }

    private void close(RunboardConnection connection, CloseStatus status) {
        connections.remove(connection.session.getId());
        try {
            connection.session.close(status);
        } catch (Exception ex) {
            logger.debug("Error closing connection to runboard {}", connection.runboardKey, ex);
        }
    }

    private Map<String, Object> notificationToMap(DispatchNotification notification) {
        final Map<String, Object> map = new HashMap<>();
        map.put("id", notification.getId());
        map.put("timestamp", notification.getTimestamp().toEpochMilli());
        map.put("assignment_id", notification.getAssignment().getId());
        map.put("assignment_type_code", notification.getAssignmentType().getCode());
        map.put("assignment_type_descr", notification.getAssignmentType().getDescription());
        map.put("urgency", notification.getUrgency());
        map.put("municipality", notification.getMunicipality().getName());
        map.put("address", notification.getAddress());
        map.put("description", notification.getDescription());
        map.put("resources", notification.getAssignedResources().stream().map(Resource::getCallSign).collect(Collectors.toSet()));
        map.put("language", language);
        return map;
    }

    private static final class RunboardConnection {

        private final String runboardKey;
        private final WebSocketSession session;
        private volatile Instant lastSeen = Instant.now();

        private RunboardConnection(String runboardKey, WebSocketSession session) {
            this.runboardKey = runboardKey;
            this.session = session;
        }

        private void touch() {
            lastSeen = Instant.now();
        }
    }
}
