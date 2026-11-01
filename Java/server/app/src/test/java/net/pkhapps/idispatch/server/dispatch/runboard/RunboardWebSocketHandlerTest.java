package net.pkhapps.idispatch.server.dispatch.runboard;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentQueries;
import net.pkhapps.idispatch.server.dispatch.AssignmentNotReadyForDispatchException;
import net.pkhapps.idispatch.server.dispatch.DispatchService;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.TestData.GivenAssignmentType;
import net.pkhapps.idispatch.server.ui.TestData.GivenMunicipality;
import net.pkhapps.idispatch.server.ui.TestData.GivenNotification;
import net.pkhapps.idispatch.server.ui.TestData.GivenResource;
import net.pkhapps.idispatch.server.ui.TestData.GivenRunboard;
import net.pkhapps.idispatch.server.ui.RunAs;
import net.pkhapps.idispatch.server.ui.TestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Talks to the runboard WebSocket endpoint of a running server, the way the runboard application does.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "idispatch.runboard.heartbeat-interval=200ms")
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class RunboardWebSocketHandlerTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    int port;
    @Autowired
    TestData testData;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    AssignmentQueries assignmentQueries;
    @Autowired
    DispatchService dispatchService;
    @Autowired
    ResourceStatusService resourceStatusService;

    private final List<TestClient> clients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        clients.forEach(TestClient::abort);
    }

    @Test
    void waitingNotifications_areSentOnConnect_andAcknowledgementCreatesReceipt() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var destination = runboardDestination(resource);
        var municipality = testData.municipality(unique("Municipality"));
        var type = testData.assignmentType(unique("AT"), "Test assignment type");
        var notification = dispatch(municipality, type, resource);

        var client = connect(destination.runboardKey());
        var message = client.nextMessage();

        assertThat(message.path("type").asString()).isEqualTo("notifications");
        var sent = message.path("notifications").get(0);
        assertThat(sent.path("id").asLong()).isEqualTo(notification.id());
        assertThat(sent.path("address").asString()).isEqualTo(notification.address());
        assertThat(sent.path("municipality").asString()).isEqualTo(municipality.nameFi());
        assertThat(sent.path("assignment_type_code").asString()).isEqualTo(type.code());
        assertThat(sent.path("urgency").asString()).isEqualTo("A");
        assertThat(sent.path("resources").get(0).asString()).isEqualTo(resource.callSign());
        assertThat(sent.path("language").asString()).isEqualTo("fi");

        client.send("{\"type\":\"ack\",\"id\":" + notification.id() + "}");

        awaitReceipt(notification, destination);
    }

    @Test
    void eachRunboard_getsTheNotificationInItsOwnLanguage() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var finnish = runboardDestination(resource);
        var swedish = testData.runboard(resource, Bilingual.SWEDISH);
        var municipality = testData.municipality(unique("Parainen "), unique("Pargas "));
        var type = testData.assignmentType(unique("AT"), "Rakennuspalo", "Byggnadsbrand");
        dispatch(municipality, type, resource);

        var inFinnish = connect(finnish.runboardKey()).nextMessage().path("notifications").get(0);
        var inSwedish = connect(swedish.runboardKey()).nextMessage().path("notifications").get(0);

        assertThat(inFinnish.path("language").asString()).isEqualTo("fi");
        assertThat(inFinnish.path("municipality").asString()).isEqualTo(municipality.nameFi());
        assertThat(inFinnish.path("assignment_type_descr").asString()).isEqualTo("Rakennuspalo");
        assertThat(inSwedish.path("language").asString()).isEqualTo("sv");
        assertThat(inSwedish.path("municipality").asString()).isEqualTo(municipality.nameSv());
        assertThat(inSwedish.path("assignment_type_descr").asString()).isEqualTo("Byggnadsbrand");
    }

    @Test
    void newNotifications_arePushedOnlyToRunboardsTheyAreFor() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var destination = runboardDestination(resource);
        var otherDestination = runboardDestination(testData.resource(unique("R"), ResourceState.AVAILABLE));
        var client = connect(destination.runboardKey());
        var otherClient = connect(otherDestination.runboardKey());

        var notification = dispatch(resource);

        assertThat(client.nextMessage().path("notifications").get(0).path("id").asLong()).isEqualTo(notification.id());
        assertThat(otherClient.messages.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void acknowledgedNotifications_areNotSentAgainOnReconnect() throws Exception {
        var resource = testData.resource(unique("R"), ResourceState.AVAILABLE);
        var destination = runboardDestination(resource);
        var notification = dispatch(resource);
        var client = connect(destination.runboardKey());
        client.nextMessage();
        client.send("{\"type\":\"ack\",\"id\":" + notification.id() + "}");
        awaitReceipt(notification, destination);
        client.abort();

        var reconnected = connect(destination.runboardKey());

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

    private GivenRunboard runboardDestination(GivenResource resource) {
        return testData.runboard(resource, Bilingual.FINNISH);
    }

    private GivenNotification dispatch(GivenResource resource) {
        return dispatch(testData.municipality(unique("Municipality")),
                testData.assignmentType(unique("AT"), "Test assignment type"), resource);
    }

    /**
     * Opens an assignment and dispatches the resource to it the way a dispatcher does, which sends the notification to
     * the resource's destinations.
     */
    private GivenNotification dispatch(GivenMunicipality municipality, GivenAssignmentType type,
                                       GivenResource resource) {
        var assignmentId = testData.openAssignment(municipality, type, unique("Address "));
        RunAs.dispatcher(() -> {
            resourceStatusService.setResourceAssignment(new ResourceId(resource.id()), new AssignmentId(assignmentId));
            try {
                return dispatchService.dispatchAllResources(
                        assignmentQueries.findById(new AssignmentId(assignmentId)).orElseThrow());
            } catch (AssignmentNotReadyForDispatchException ex) {
                throw new AssertionError("The assignment could not be dispatched", ex);
            }
        });
        return testData.notificationsFor(assignmentId).getFirst();
    }

    private void awaitReceipt(GivenNotification notification, GivenRunboard destination) throws InterruptedException {
        var deadline = System.nanoTime() + Duration.ofSeconds(TIMEOUT_SECONDS).toNanos();
        while (testData.receiptsFor(notification).isEmpty() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(testData.receiptsFor(notification)).containsExactly(destination.id());
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
