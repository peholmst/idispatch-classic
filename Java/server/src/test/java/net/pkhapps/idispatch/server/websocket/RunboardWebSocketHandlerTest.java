package net.pkhapps.idispatch.server.websocket;

import net.pkhapps.idispatch.server.entity.*;
import net.pkhapps.idispatch.server.entity.repository.AssignmentRepository;
import net.pkhapps.idispatch.server.entity.repository.DestinationRepository;
import net.pkhapps.idispatch.server.entity.repository.DispatchNotificationRepository;
import net.pkhapps.idispatch.server.entity.repository.ReceiptRepository;
import net.pkhapps.idispatch.server.events.DispatchNotificationSent;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Talks to the runboard WebSocket endpoint of a running server, the way the runboard application does.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "idispatch.runboard.heartbeat-interval=200ms")
@Import(TestData.class)
class RunboardWebSocketHandlerTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    int port;
    @Autowired
    TestData testData;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    ApplicationEventPublisher eventPublisher;
    @Autowired
    AssignmentRepository assignmentRepository;
    @Autowired
    DestinationRepository destinationRepository;
    @Autowired
    DispatchNotificationRepository dispatchNotificationRepository;
    @Autowired
    ReceiptRepository receiptRepository;

    private final List<TestClient> clients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        clients.forEach(TestClient::abort);
    }

    @Test
    void waitingNotifications_areSentOnConnect_andAcknowledgementCreatesReceipt() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var destination = runboardDestination(resource);
        var notification = dispatch(resource, destination);

        var client = connect(destination.getRunboardKey());
        var message = client.nextMessage();

        assertThat(message.path("type").asString()).isEqualTo("notifications");
        var sent = message.path("notifications").get(0);
        assertThat(sent.path("id").asLong()).isEqualTo(notification.getId());
        assertThat(sent.path("address").asString()).isEqualTo(notification.getAddress());
        assertThat(sent.path("municipality").asString()).isEqualTo(notification.getMunicipality().getName());
        assertThat(sent.path("assignment_type_code").asString()).isEqualTo(notification.getAssignmentType().getCode());
        assertThat(sent.path("urgency").asString()).isEqualTo(notification.getUrgency().name());
        assertThat(sent.path("resources").get(0).asString()).isEqualTo(resource.getCallSign());

        client.send("{\"type\":\"ack\",\"id\":" + notification.getId() + "}");

        awaitReceipt(notification, destination);
    }

    @Test
    void newNotifications_arePushedOnlyToRunboardsTheyAreFor() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var destination = runboardDestination(resource);
        var otherDestination = runboardDestination(testData.resource(unique("R"), ResourceState.AVAILABLE));
        var client = connect(destination.getRunboardKey());
        var otherClient = connect(otherDestination.getRunboardKey());

        var notification = dispatch(resource, destination);

        assertThat(client.nextMessage().path("notifications").get(0).path("id").asLong()).isEqualTo(notification.getId());
        assertThat(otherClient.messages.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void acknowledgedNotifications_areNotSentAgainOnReconnect() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var destination = runboardDestination(resource);
        var notification = dispatch(resource, destination);
        var client = connect(destination.getRunboardKey());
        client.nextMessage();
        client.send("{\"type\":\"ack\",\"id\":" + notification.getId() + "}");
        awaitReceipt(notification, destination);
        client.abort();

        var reconnected = connect(destination.getRunboardKey());

        assertThat(reconnected.messages.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void heartbeat_pingsRunboards_andKeepsRespondingConnectionsOpen() throws Exception {
        var client = connect(unique("KEY"));

        // The timeout is three heartbeat intervals (600 ms); the client answers every ping with a pong.
        Thread.sleep(1500);

        assertThat(client.pings.get()).isGreaterThanOrEqualTo(3);
        assertThat(client.closed).isNotDone();
    }

    @Test
    void connectionWithoutRunboardKey_isClosed() throws Exception {
        var client = connect(null);

        assertThat(client.closed.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(1008);
    }

    private RunboardDestination runboardDestination(Resource resource) {
        var destination = new RunboardDestination();
        destination.setRunboardKey(unique("KEY"));
        destination.setResources(Set.of(resource));
        return destinationRepository.saveAndFlush(destination);
    }

    private DispatchNotification dispatch(Resource resource, Destination destination) {
        var assignment = new Assignment();
        assignment.setMunicipality(testData.municipality(unique("Municipality")));
        assignment.setType(testData.assignmentType(unique("AT"), "Test assignment type"));
        assignment.setUrgency(AssignmentUrgency.A);
        assignment.setAddress(unique("Address"));
        assignment = assignmentRepository.saveAndFlush(assignment);
        var notification = dispatchNotificationRepository.saveAndFlush(
                new DispatchNotification(assignment, Set.of(resource), Set.of(destination)));
        eventPublisher.publishEvent(new DispatchNotificationSent(this, notification));
        return notification;
    }

    private void awaitReceipt(DispatchNotification notification, Destination destination) throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(TIMEOUT_SECONDS).toNanos();
        while (receiptRepository.findByNotification(notification).isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(receiptRepository.findByNotification(notification))
                .extracting(receipt -> receipt.getDestination().getId())
                .containsExactly(destination.getId());
    }

    private TestClient connect(String runboardKey) throws Exception {
        var uri = "ws://localhost:" + port + "/idispatch/ws/runboard" + (runboardKey == null ? "" : "?runboardKey=" + runboardKey);
        var client = new TestClient();
        client.webSocket = HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create(uri), client)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        clients.add(client);
        return client;
    }

    private class TestClient implements WebSocket.Listener {

        final BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
        final CompletableFuture<Integer> closed = new CompletableFuture<>();
        final AtomicInteger pings = new AtomicInteger();
        private final StringBuilder buffer = new StringBuilder();
        WebSocket webSocket;

        JsonNode nextMessage() throws InterruptedException {
            var message = messages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(message).as("message from server").isNotNull();
            return message;
        }

        void send(String text) throws Exception {
            webSocket.sendText(text, true).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        void abort() {
            webSocket.abort();
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                messages.add(jsonMapper.readTree(buffer.toString()));
                buffer.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            pings.incrementAndGet();
            return WebSocket.Listener.super.onPing(webSocket, message);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
        }
    }
}
