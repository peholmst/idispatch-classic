package net.pkhapps.idispatch.server.security;

import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The browser can load every stylesheet that {@code styles.css} imports. Vaadin permits only the static resources it
 * knows of and refuses the rest, even to a logged-in user, so an imported stylesheet that security does not permit is
 * silently left out and its styles never apply.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class})
class StylesheetAccessTest {

    private static final Pattern IMPORT = Pattern.compile("@import\\s+\"([^\"]+)\"");

    @LocalServerPort
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void everyStylesheetImportedByStylesCssCanBeLoaded() throws Exception {
        var styles = get("styles.css");
        assertThat(styles.statusCode()).isEqualTo(200);

        var imports = IMPORT.matcher(styles.body()).results().map(match -> match.group(1)).toList();
        assertThat(imports).isNotEmpty();
        for (var path : imports) {
            assertThat(get(path).statusCode()).as(path).isEqualTo(200);
        }
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/idispatch/" + path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
