package net.pkhapps.idispatch.server.dispatch.mobile;

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
import net.pkhapps.idispatch.server.ui.RunAs;
import net.pkhapps.idispatch.server.ui.TestData;
import net.pkhapps.idispatch.server.ui.TestData.GivenAssignmentType;
import net.pkhapps.idispatch.server.ui.TestData.GivenMunicipality;
import net.pkhapps.idispatch.server.ui.TestData.GivenNotification;
import net.pkhapps.idispatch.server.ui.TestData.GivenResource;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The event stream the mobile app keeps open, read over HTTP the way the app reads it: it starts with the unit and the
 * notifications the app has not confirmed, tells every change of the unit's status and every new notification, and
 * ends when the destination no longer has the key it was opened with.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "idispatch.mobile.heartbeat-interval=200ms")
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class MobileEventStreamTest {

    private static final long TIMEOUT_SECONDS = 5;
    /**
     * Put on the queue when the stream ends.
     */
    private static final String END = "<end>";

    @LocalServerPort
    int port;
    @Autowired
    TestData given;
    @Autowired
    JsonMapper jsonMapper;
    @Autowired
    ResourceStatusService resourceStatusService;
    @Autowired
    DispatchService dispatchService;
    @Autowired
    AssignmentQueries assignmentQueries;

    private final HttpClient client = HttpClient.newHttpClient();
    private final List<Thread> readers = new ArrayList<>();

    @AfterEach
    void stopReading() {
        readers.forEach(Thread::interrupt);
    }

    @Test
    void streamStartsWithTheUnit() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AT_STATION);
        var stream = open(given.mobile(resource, Bilingual.FINNISH).key());

        var unit = nextUnit(stream);

        assertThat(unit.get("callSign").asString()).isEqualTo(resource.callSign());
        assertThat(unit.get("status").get("state").asString()).isEqualTo("AT_STATION");
    }

    @Test
    void statusChangeMadeByADispatcherReachesTheApp() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AT_STATION);
        var stream = open(given.mobile(resource, Bilingual.SWEDISH).key());
        nextUnit(stream);

        RunAs.dispatcher(() -> resourceStatusService.setResourceStateManually(new ResourceId(resource.id()),
                ResourceState.OUT_OF_SERVICE));

        var unit = nextUnit(stream);
        assertThat(unit.get("status").get("state").asString()).isEqualTo("OUT_OF_SERVICE");
        assertThat(unit.get("status").get("label").asString()).isEqualTo("Ur tjänst");
    }

    @Test
    void statusChangesOfOtherUnitsDoNotReachTheApp() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AT_STATION);
        var other = given.resource(unique("CS"), ResourceState.AT_STATION);
        var stream = open(given.mobile(resource, Bilingual.FINNISH).key());
        nextUnit(stream);

        RunAs.dispatcher(() -> resourceStatusService.setResourceStateManually(new ResourceId(other.id()),
                ResourceState.AVAILABLE));
        RunAs.dispatcher(() -> resourceStatusService.setResourceStateManually(new ResourceId(resource.id()),
                ResourceState.AVAILABLE));

        assertThat(nextUnit(stream).get("callSign").asString()).isEqualTo(resource.callSign());
    }

    @Test
    void streamEndsWhenTheDestinationGetsANewKey() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AT_STATION), Bilingual.FINNISH);
        var stream = open(mobile.key());
        nextUnit(stream);

        given.newKeyFor(mobile);

        assertThat(next(stream, line -> line.equals(END))).isEqualTo(END);
    }

    @Test
    void streamEndsWhenTheDestinationIsDeleted() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AT_STATION), Bilingual.FINNISH);
        var stream = open(mobile.key());
        nextUnit(stream);

        given.deleteDestination(mobile.id());

        assertThat(next(stream, line -> line.equals(END))).isEqualTo(END);
    }

    @Test
    void streamIsRefusedWithoutAValidKey() throws Exception {
        var response = client.send(request("x".repeat(43)).build(), HttpResponse.BodyHandlers.discarding());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void notificationForTheUnitReachesTheApp_inTheDestinationsLanguage() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var stream = open(given.mobile(resource, Bilingual.SWEDISH).key());
        nextUnit(stream);
        var municipality = given.municipality(unique("Parainen "), unique("Pargas "));
        var type = given.assignmentType(unique("AT"), "Rakennuspalo", "Byggnadsbrand");

        var sent = dispatch(municipality, type, resource);

        var notification = nextNotification(stream);
        assertThat(notification.get("id").asLong()).isEqualTo(sent.id());
        assertThat(notification.get("address").asString()).isEqualTo(sent.address());
        assertThat(notification.get("municipality").asString()).isEqualTo(municipality.nameSv());
        assertThat(notification.get("typeCode").asString()).isEqualTo(type.code());
        assertThat(notification.get("type").asString()).isEqualTo("Byggnadsbrand");
        assertThat(notification.get("urgency").asString()).isEqualTo("A");
        assertThat(notification.get("resources").get(0).asString()).isEqualTo(resource.callSign());
        assertThat(notification.get("timestamp").asString()).isNotBlank();
    }

    @Test
    void notificationsForOtherUnitsDoNotReachTheApp() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var stream = open(given.mobile(resource, Bilingual.FINNISH).key());
        nextUnit(stream);

        dispatch(given.resource(unique("CS"), ResourceState.AVAILABLE));
        var own = dispatch(resource);

        assertThat(nextNotification(stream).get("id").asLong()).isEqualTo(own.id());
    }

    @Test
    void notificationSentWhileTheAppWasAwayArrivesWhenItConnects_untilItIsConfirmed() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(resource, Bilingual.FINNISH);
        var sent = dispatch(resource);

        var first = open(mobile.key());
        nextUnit(first);
        assertThat(nextNotification(first).get("id").asLong()).isEqualTo(sent.id());
        var again = open(mobile.key());
        nextUnit(again);
        assertThat(nextNotification(again).get("id").asLong()).as("Not confirmed yet").isEqualTo(sent.id());

        assertThat(confirm(mobile.key(), sent.id()).statusCode()).isEqualTo(204);

        assertThat(given.receiptsFor(sent)).containsExactly(mobile.id());
        var afterConfirming = open(mobile.key());
        nextUnit(afterConfirming);
        var later = dispatch(resource);
        assertThat(nextNotification(afterConfirming).get("id").asLong()).as("Not the confirmed one")
                .isEqualTo(later.id());
    }

    @Test
    void notificationSentLongerAgoThanTheRedeliveryWindowIsNotSentAgain() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(resource, Bilingual.FINNISH);
        var old = dispatch(resource);
        given.sentEarlier(old, Duration.ofHours(25));

        var stream = open(mobile.key());
        nextUnit(stream);
        var later = dispatch(resource);

        assertThat(nextNotification(stream).get("id").asLong()).isEqualTo(later.id());
    }

    @Test
    void confirmingTwiceStoresOneReceipt() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(resource, Bilingual.FINNISH);
        var sent = dispatch(resource);

        assertThat(confirm(mobile.key(), sent.id()).statusCode()).isEqualTo(204);
        assertThat(confirm(mobile.key(), sent.id()).statusCode()).isEqualTo(204);

        assertThat(given.receiptsFor(sent)).containsExactly(mobile.id());
    }

    @Test
    void notificationOfAnotherDestinationCannotBeConfirmed() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);
        var others = dispatch(given.resource(unique("CS"), ResourceState.AVAILABLE));

        assertThat(confirm(mobile.key(), others.id()).statusCode()).isEqualTo(404);
        assertThat(confirm("x".repeat(43), others.id()).statusCode()).isEqualTo(401);

        assertThat(given.receiptsFor(others)).isEmpty();
    }

    private HttpResponse<Void> confirm(String key, long notification) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port
                        + "/idispatch/api/mobile/notifications/" + notification + "/receipt"))
                .header("Authorization", "Bearer " + key)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build(), HttpResponse.BodyHandlers.discarding());
    }

    private GivenNotification dispatch(GivenResource resource) {
        return dispatch(given.municipality(unique("Municipality")),
                given.assignmentType(unique("AT"), "Test assignment type"), resource);
    }

    /**
     * Opens an assignment and dispatches the resource to it the way a dispatcher does, which sends the notification to
     * the resource's destinations.
     */
    private GivenNotification dispatch(GivenMunicipality municipality, GivenAssignmentType type,
                                       GivenResource resource) {
        var assignmentId = given.openAssignment(municipality, type, unique("Address "));
        RunAs.dispatcher(() -> {
            // Back from any earlier assignment, so that it can be dispatched again
            resourceStatusService.setResourceStateManually(new ResourceId(resource.id()), ResourceState.AVAILABLE);
            resourceStatusService.setResourceAssignment(new ResourceId(resource.id()), new AssignmentId(assignmentId));
            try {
                return dispatchService.dispatchAllResources(
                        assignmentQueries.findById(new AssignmentId(assignmentId)).orElseThrow());
            } catch (AssignmentNotReadyForDispatchException ex) {
                throw new AssertionError("The assignment could not be dispatched", ex);
            }
        });
        var notifications = given.notificationsFor(assignmentId);
        assertThat(notifications).as("The notification of the dispatch").hasSize(1);
        return notifications.getFirst();
    }

    /**
     * Waits for the next {@code notification} event and returns its data.
     */
    private JsonNode nextNotification(BlockingQueue<String> stream) throws InterruptedException {
        next(stream, line -> line.equals("event:notification"));
        var data = next(stream, line -> line.startsWith("data:"));
        return jsonMapper.readTree(data.substring("data:".length()));
    }

    /**
     * Opens the stream and reads its lines on a thread of their own.
     */
    private BlockingQueue<String> open(String key) throws Exception {
        var response = client.send(request(key).build(), HttpResponse.BodyHandlers.ofLines());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type ->
                assertThat(type).startsWith("text/event-stream"));
        var lines = new LinkedBlockingQueue<String>();
        readers.add(Thread.ofVirtual().start(() -> {
            try (var body = response.body()) {
                body.forEach(lines::add);
            } catch (RuntimeException ex) {
                // Interrupted at the end of the test
            }
            lines.add(END);
        }));
        return lines;
    }

    private HttpRequest.Builder request(String key) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/idispatch/api/mobile/events"))
                .header("Authorization", "Bearer " + key)
                .header("Accept", "text/event-stream")
                .GET();
    }

    /**
     * Waits for the next {@code unit} event and returns its data.
     */
    private JsonNode nextUnit(BlockingQueue<String> stream) throws InterruptedException {
        next(stream, line -> line.equals("event:unit"));
        var data = next(stream, line -> line.startsWith("data:"));
        return jsonMapper.readTree(data.substring("data:".length()));
    }

    /**
     * Returns the next line that is wanted, skipping others such as heartbeats, and fails if none comes before the
     * timeout or the stream ends first.
     */
    private static String next(BlockingQueue<String> stream, java.util.function.Predicate<String> wanted)
            throws InterruptedException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        while (true) {
            var line = stream.poll(deadline - System.nanoTime(), TimeUnit.NANOSECONDS);
            assertThat(line).as("The wanted line before the timeout").isNotNull();
            if (wanted.test(line)) {
                return line;
            }
            assertThat(line).as("The stream ended").isNotEqualTo(END);
        }
    }
}
