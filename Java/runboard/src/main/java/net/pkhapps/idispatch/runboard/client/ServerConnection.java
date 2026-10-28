package net.pkhapps.idispatch.runboard.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Keeps a WebSocket connection to the server open, receives dispatch notifications through it and acknowledges them.
 * <p>
 * Both ends ping each other: the connection pings the server every heartbeat interval, and the server pings the
 * connection. If nothing has been heard from the server in three heartbeat intervals, the connection is considered
 * dead. Whenever the connection is lost, it is reopened with an increasing delay.
 */
public class ServerConnection {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final long MAX_RECONNECT_DELAY_MILLISECONDS = 30000;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("server-connection").daemon().factory());
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final URI uri;
    private final long heartbeatIntervalMilliseconds;
    private final Callback callback;
    private WebSocket webSocket;
    private CompletableFuture<?> pendingSend = CompletableFuture.completedFuture(null);
    private volatile long lastReceived;
    private int failedAttempts = 0;
    private boolean running = false;
    private ScheduledFuture<?> heartbeatHandle;

    public ServerConnection(Configuration configuration, Callback callback) {
        this.uri = URI.create(configuration.getServerUrl() + "?runboardKey="
                + URLEncoder.encode(configuration.getRunboardKey(), StandardCharsets.UTF_8));
        this.heartbeatIntervalMilliseconds = configuration.getHeartbeatIntervalMilliseconds();
        this.callback = callback;
    }

    public synchronized void start() {
        if (!running) {
            running = true;
            scheduler.execute(this::connect);
            heartbeatHandle = scheduler.scheduleWithFixedDelay(this::heartbeat,
                    heartbeatIntervalMilliseconds,
                    heartbeatIntervalMilliseconds,
                    TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void stop() {
        if (running) {
            running = false;
            heartbeatHandle.cancel(false);
            if (webSocket != null) {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Runboard stopped");
                webSocket = null;
            }
        }
    }

    public synchronized boolean isRunning() {
        return running;
    }

    private void connect() {
        synchronized (this) {
            if (!running || webSocket != null) {
                return;
            }
        }
        logger.info("Connecting to {}", uri);
        httpClient.newWebSocketBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .buildAsync(uri, new Listener())
                .whenComplete((ws, ex) -> {
                    if (ex != null) {
                        final Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                        logger.error("Could not connect to the server: {}", cause.toString());
                        reconnectLater(cause instanceof WebSocketHandshakeException
                                ? DispatcherClientException.ErrorCode.UNKNOWN_SERVER_ERROR
                                : DispatcherClientException.ErrorCode.COMMUNICATION_ERROR);
                    }
                });
    }

    private synchronized void connected(WebSocket ws) {
        if (!running) {
            ws.abort();
            return;
        }
        logger.info("Connected to the server");
        webSocket = ws;
        pendingSend = CompletableFuture.completedFuture(null);
        lastReceived = System.currentTimeMillis();
        failedAttempts = 0;
        callback.clearErrorCode();
    }

    private synchronized void disconnected(WebSocket ws, String reason) {
        if (webSocket != ws) {
            return; // Already handled, or a connection that has been replaced
        }
        logger.error("Lost connection to the server: {}", reason);
        webSocket = null;
        ws.abort();
        reconnectLater(DispatcherClientException.ErrorCode.COMMUNICATION_ERROR);
    }

    private synchronized void reconnectLater(DispatcherClientException.ErrorCode errorCode) {
        callback.setErrorCode(errorCode);
        if (running) {
            final long delay = Math.min(1000L << Math.min(failedAttempts, 5), MAX_RECONNECT_DELAY_MILLISECONDS);
            failedAttempts++;
            logger.debug("Reconnecting in {} ms", delay);
            scheduler.schedule(this::connect, delay, TimeUnit.MILLISECONDS);
        }
    }

    private void heartbeat() {
        final WebSocket ws;
        synchronized (this) {
            ws = webSocket;
        }
        if (ws == null) {
            return;
        }
        final long silence = System.currentTimeMillis() - lastReceived;
        if (silence > 3 * heartbeatIntervalMilliseconds) {
            disconnected(ws, "nothing heard from the server in " + silence + " ms");
        } else {
            send(ws, w -> w.sendPing(ByteBuffer.allocate(0)));
        }
    }

    private void acknowledge(WebSocket ws, Notification notification) {
        logger.info("Acknowledging notification {}", notification.getId());
        try {
            final String message = objectMapper.writeValueAsString(Map.of("type", "ack", "id", notification.getId()));
            send(ws, w -> w.sendText(message, true));
        } catch (JsonProcessingException ex) {
            logger.error("Could not acknowledge notification {}", notification.getId(), ex);
        }
    }

    /**
     * Sends one message at a time, since a {@link WebSocket} does not allow a send to start before the previous one
     * has completed.
     */
    private synchronized void send(WebSocket ws, Function<WebSocket, CompletableFuture<WebSocket>> action) {
        if (webSocket != ws) {
            return;
        }
        pendingSend = pendingSend
                .exceptionally(ex -> null)
                .thenCompose(v -> action.apply(ws))
                .whenComplete((v, ex) -> {
                    if (ex != null) {
                        disconnected(ws, "could not send message: " + ex);
                    }
                });
    }

    private void messageReceived(WebSocket ws, String text) {
        final Map<String, Object> message;
        try {
            message = objectMapper.readValue(text, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            logger.error("Ignoring malformed message from the server: {}", text);
            return;
        }
        if ("notifications".equals(message.get("type"))) {
            @SuppressWarnings("unchecked")
            final List<Notification> notifications = ((List<Map<String, Object>>) message.get("notifications"))
                    .stream()
                    .map(Notification::new)
                    .toList();
            logger.info("Received notifications {}", notifications);
            notifications.forEach(notification -> acknowledge(ws, notification));
            callback.notificationsReceived(notifications);
        } else {
            logger.warn("Ignoring unknown message from the server: {}", text);
        }
    }

    private class Listener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            connected(ws);
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            lastReceived = System.currentTimeMillis();
            buffer.append(data);
            if (last) {
                final String text = buffer.toString();
                buffer.setLength(0);
                try {
                    messageReceived(ws, text);
                } catch (RuntimeException ex) {
                    logger.error("Error handling message from the server: {}", text, ex);
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            lastReceived = System.currentTimeMillis();
            return WebSocket.Listener.super.onPing(ws, message);
        }

        @Override
        public CompletionStage<?> onPong(WebSocket ws, ByteBuffer message) {
            lastReceived = System.currentTimeMillis();
            return WebSocket.Listener.super.onPong(ws, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            disconnected(ws, "closed by the server with status " + statusCode + " " + reason);
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            disconnected(ws, error.toString());
        }
    }

    public interface Callback {

        void clearErrorCode();

        void setErrorCode(DispatcherClientException.ErrorCode errorCode);

        void notificationsReceived(Collection<Notification> notifications);
    }
}
