package net.pkhapps.idispatch.server.dispatch.runboard;

import jakarta.persistence.EntityManager;
import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentQueries;
import net.pkhapps.idispatch.server.dispatch.Destination;
import net.pkhapps.idispatch.server.dispatch.DestinationRepository;
import net.pkhapps.idispatch.server.dispatch.DispatchNotification;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationRepository;
import net.pkhapps.idispatch.server.dispatch.DispatchNotificationSent;
import net.pkhapps.idispatch.server.dispatch.ReceiptRepository;
import net.pkhapps.idispatch.server.resource.Resource;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.TestData.GivenAssignmentType;
import net.pkhapps.idispatch.server.ui.TestData.GivenMunicipality;
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
import java.util.Locale;
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
    ApplicationEventPublisher eventPublisher;
    @Autowired
    AssignmentQueries assignmentQueries;
    @Autowired
    DestinationRepository destinationRepository;
    @Autowired
    DispatchNotificationRepository dispatchNotificationRepository;
    @Autowired
    ReceiptRepository receiptRepository;
    @Autowired
    EntityManager entityManager;

    private final List<TestClient> clients = new ArrayList<>();

    @AfterEach
    void closeClients() {
        clients.forEach(TestClient::abort);
    }

    @Test
    void waitingNotifications_areSentOnConnect_andAcknowledgementCreatesReceipt() throws Exception {
        var resource = entity(Resource.class, testData.resource(unique("R"), ResourceState.AVAILABLE).id());
        var destination = runboardDestination(resource);
        var municipality = testData.municipality(unique("Municipality"));
        var type = testData.assignmentType(unique("AT"), "Test assignment type");
        var notification = dispatch(assignment(municipality, type), resource, Set.of(destination));

        var client = connect(destination.getRunboardKey());
        var message = client.nextMessage();

        assertThat(message.path("type").asString()).isEqualTo("notifications");
        var sent = message.path("notifications").get(0);
        assertThat(sent.path("id").asLong()).isEqualTo(notification.getId());
        assertThat(sent.path("address").asString()).isEqualTo(notification.getAddress());
        assertThat(sent.path("municipality").asString()).isEqualTo(municipality.nameFi());
        assertThat(sent.path("assignment_type_code").asString()).isEqualTo(type.code());
        assertThat(sent.path("urgency").asString()).isEqualTo(notification.getUrgency().name());
        assertThat(sent.path("resources").get(0).asString()).isEqualTo(resource.getCallSign());
        assertThat(sent.path("language").asString()).isEqualTo("fi");

        client.send("{\"type\":\"ack\",\"id\":" + notification.getId() + "}");

        awaitReceipt(notification, destination);
    }

    @Test
    void eachRunboard_getsTheNotificationInItsOwnLanguage() throws Exception {
        var resource = entity(Resource.class, testData.resource(unique("R"), ResourceState.AVAILABLE).id());
        var finnish = runboardDestination(resource);
        var swedish = runboardDestination(resource, Bilingual.SWEDISH);
        var municipality = testData.municipality(unique("Parainen "), unique("Pargas "));
        var type = testData.assignmentType(unique("AT"), "Rakennuspalo", "Byggnadsbrand");
        dispatch(assignment(municipality, type), resource, Set.of(finnish, swedish));

        var inFinnish = connect(finnish.getRunboardKey()).nextMessage().path("notifications").get(0);
        var inSwedish = connect(swedish.getRunboardKey()).nextMessage().path("notifications").get(0);

        assertThat(inFinnish.path("language").asString()).isEqualTo("fi");
        assertThat(inFinnish.path("municipality").asString()).isEqualTo(municipality.nameFi());
        assertThat(inFinnish.path("assignment_type_descr").asString()).isEqualTo("Rakennuspalo");
        assertThat(inSwedish.path("language").asString()).isEqualTo("sv");
        assertThat(inSwedish.path("municipality").asString()).isEqualTo(municipality.nameSv());
        assertThat(inSwedish.path("assignment_type_descr").asString()).isEqualTo("Byggnadsbrand");
    }

    @Test
    void newNotifications_arePushedOnlyToRunboardsTheyAreFor() throws Exception {
        var resource = entity(Resource.class, testData.resource(unique("R"), ResourceState.AVAILABLE).id());
        var destination = runboardDestination(resource);
        var otherDestination = runboardDestination(entity(Resource.class, testData.resource(unique("R"), ResourceState.AVAILABLE).id()));
        var client = connect(destination.getRunboardKey());
        var otherClient = connect(otherDestination.getRunboardKey());

        var notification = dispatch(resource, destination);

        assertThat(client.nextMessage().path("notifications").get(0).path("id").asLong()).isEqualTo(notification.getId());
        assertThat(otherClient.messages.poll(500, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void acknowledgedNotifications_areNotSentAgainOnReconnect() throws Exception {
        var resource = entity(Resource.class, testData.resource(unique("R"), ResourceState.AVAILABLE).id());
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

    /**
     * Loads a fixture as the entity that the dispatch classes still work with, detached like the ones the services
     * return.
     */
    private <T> T entity(Class<T> type, long id) {
        return entityManager.find(type, id);
    }

    private RunboardDestination runboardDestination(Resource resource) {
        return runboardDestination(resource, Bilingual.FINNISH);
    }

    private RunboardDestination runboardDestination(Resource resource, Locale locale) {
        var destination = new RunboardDestination();
        destination.setRunboardKey(unique("KEY"));
        destination.setLocale(locale);
        destination.setResources(Set.of(resource));
        return destinationRepository.saveAndFlush(destination);
    }

    private DispatchNotification dispatch(Resource resource, Destination destination) {
        var assignment = assignment(testData.municipality(unique("Municipality")),
                testData.assignmentType(unique("AT"), "Test assignment type"));
        return dispatch(assignment, resource, Set.of(destination));
    }

    private Assignment assignment(GivenMunicipality municipality, GivenAssignmentType type) {
        var id = testData.openAssignment(municipality, type, unique("Address"));
        return assignmentQueries.findById(new AssignmentId(id)).orElseThrow();
    }

    private DispatchNotification dispatch(Assignment assignment, Resource resource, Set<Destination> destinations) {
        var notification = dispatchNotificationRepository.saveAndFlush(
                new DispatchNotification(assignment, Set.of(resource), destinations));
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
