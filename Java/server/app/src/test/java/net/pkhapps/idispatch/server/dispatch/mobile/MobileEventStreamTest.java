package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.shared.Bilingual;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The event stream the mobile app keeps open, read over HTTP the way the app reads it: it starts with the unit, tells
 * every change of the unit's status, and ends when the destination no longer has the key it was opened with.
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

    private static String next(BlockingQueue<String> stream, java.util.function.Predicate<String> wanted)
            throws InterruptedException {
        while (true) {
            var line = stream.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertThat(line).as("A line before the timeout").isNotNull();
            if (wanted.test(line)) {
                return line;
            }
            assertThat(line).as("The stream ended").isNotEqualTo(END);
        }
    }
}
