package net.pkhapps.idispatch.server.dispatch.mobile;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.shared.Bilingual;
import net.pkhapps.idispatch.server.ui.TestData;
import net.pkhapps.idispatch.server.ui.TestData.GivenMobile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The mobile app and its API, over HTTP the way a phone reaches them: the app loads without logging in, and the API
 * answers only a request with the key of a mobile destination, about that destination's unit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestData.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class MobileAppApiTest {

    private static final Pattern ASSET = Pattern.compile("(?:src|href)=\"\\./(assets/[^\"]+)\"");

    @LocalServerPort
    int port;
    @Autowired
    TestData given;
    @Autowired
    JsonMapper jsonMapper;

    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Test
    void appGetsTheUnitOfItsDestination_inTheDestinationsLanguage() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AT_STATION);
        var mobile = given.mobile(resource, Bilingual.SWEDISH);

        var response = getUnit(mobile.key());

        assertThat(response.statusCode()).isEqualTo(200);
        var unit = json(response);
        assertThat(unit.get("callSign").asString()).isEqualTo(resource.callSign());
        assertThat(unit.get("locale").asString()).isEqualTo("sv");
        assertThat(unit.get("status").get("state").asString()).isEqualTo("AT_STATION");
        assertThat(unit.get("status").get("label").asString()).isEqualTo("På stationen");
    }

    @Test
    void finnishDestinationGetsTheStatusInFinnish() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);

        var unit = json(getUnit(mobile.key()));

        assertThat(unit.get("locale").asString()).isEqualTo("fi");
        assertThat(unit.get("status").get("label").asString()).isEqualTo("Vapaa");
    }

    @Test
    void requestWithoutAKeyIsRefused_ratherThanSentToTheLogin() throws Exception {
        var response = send(request("api/mobile/unit").GET().build());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("Location")).isEmpty();
    }

    @Test
    void keyThatNoDestinationHasIsRefused() throws Exception {
        assertThat(getUnit("x".repeat(43)).statusCode()).isEqualTo(401);
        assertThat(getUnit("not a key").statusCode()).isEqualTo(401);
    }

    @Test
    void keyInTheAddressIsNotAccepted() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);

        var response = send(request("api/mobile/unit?key=" + mobile.key()).GET().build());

        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    void oldKeyStopsWorkingWhenTheDestinationGetsANewOne() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);

        var renewed = given.newKeyFor(mobile);

        assertThat(getUnit(mobile.key()).statusCode()).isEqualTo(401);
        assertThat(getUnit(renewed.key()).statusCode()).isEqualTo(200);
    }

    @Test
    void keyOfADeletedDestinationIsRefused() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);

        given.deleteDestination(mobile.id());

        assertThat(getUnit(mobile.key()).statusCode()).isEqualTo(401);
    }

    @Test
    void deletedUnitHasNoStatus() throws Exception {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var mobile = given.mobile(resource, Bilingual.FINNISH);
        given.deleteResource(resource);

        var unit = json(getUnit(mobile.key()));

        assertThat(unit.get("callSign").asString()).isEqualTo(resource.callSign());
        assertThat(unit.get("status").isNull()).isTrue();
    }

    @Test
    void apiKeepsNoSession() throws Exception {
        var mobile = given.mobile(given.resource(unique("CS"), ResourceState.AVAILABLE), Bilingual.FINNISH);

        assertThat(getUnit(mobile.key()).headers().allValues("Set-Cookie")).isEmpty();
    }

    @Test
    void appLoadsWithoutLoggingIn() throws Exception {
        var folder = send(request("mobile").GET().build());
        assertThat(folder.statusCode()).as("The folder without a slash").isEqualTo(302);
        assertThat(folder.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).endsWith("/idispatch/mobile/"));

        var page = send(request("mobile/").GET().build());
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("<idispatch-mobile-app>");

        var assets = ASSET.matcher(page.body()).results().map(match -> match.group(1)).toList();
        assertThat(assets).isNotEmpty();
        for (var asset : assets) {
            assertThat(send(request("mobile/" + asset).GET().build()).statusCode()).as(asset).isEqualTo(200);
        }
    }

    @Test
    void restOfTheApplicationStillNeedsTheLogin() throws Exception {
        var response = send(request("").GET().build());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location")).hasValueSatisfying(location ->
                assertThat(location).contains("/oauth2/authorization/keycloak"));
    }

    private HttpResponse<String> getUnit(String key) throws IOException, InterruptedException {
        return send(request("api/mobile/unit").header("Authorization", "Bearer " + key).GET().build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/idispatch/" + path));
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode json(HttpResponse<String> response) {
        return jsonMapper.readTree(response.body());
    }
}
