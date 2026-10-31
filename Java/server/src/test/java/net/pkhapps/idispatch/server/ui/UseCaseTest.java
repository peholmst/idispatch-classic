package net.pkhapps.idispatch.server.ui;

import com.vaadin.browserless.BrowserlessUserContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import net.pkhapps.idispatch.server.TestSecurityConfiguration;
import net.pkhapps.idispatch.server.TestcontainersConfiguration;
import net.pkhapps.idispatch.server.ui.reports.HandledAssignments;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Base class for the use-case tests of the user interface. Every test gets a fresh browserless application, in which
 * every person in the story logs in as their own user and works in their own window. The application runs against the
 * full Spring context and a PostgreSQL container.
 * <p>
 * The services use {@code REQUIRES_NEW} transactions, so nothing a test does is rolled back. Tests set up their
 * starting situation with {@link TestData}, giving everything a unique name so that tests don't see each other's data.
 */
@SpringBootTest
// Every test class imports the same configuration, so that they share one Spring context and database
@Import({TestData.class, HandledAssignments.class, TestcontainersConfiguration.class, TestSecurityConfiguration.class})
public abstract class UseCaseTest {

    protected static final String KEYCLOAK_REALM = "http://keycloak.invalid/realms/idispatch";

    @Autowired
    protected ApplicationContext spring;
    @Autowired
    protected TestData given;

    protected SecuredBrowserlessApplicationContext<Authentication> app;

    @BeforeEach
    void openTheApplication() {
        app = SpringBrowserlessApplicationContext.createSecured(spring, RootView.class);
    }

    @AfterEach
    void closeTheApplication() {
        app.close();
    }

    /**
     * A user who is not logged in.
     */
    protected BrowserlessUserContext visitor() {
        return app.newUser();
    }

    /**
     * A user with the given roles ({@code DISPATCHER}, {@code ADMIN}, {@code REPORT_READER}, {@code REPORT_WRITER},
     * {@code REPORT_REVIEWER}), logged in without an ID token.
     */
    protected BrowserlessUserContext user(String username, String... roles) {
        return app.newUser(username, roles);
    }

    /**
     * A user logged in through Keycloak, with the given claims in their ID token on top of the username, and the given
     * roles.
     */
    protected BrowserlessUserContext keycloakUser(String username, Map<String, Object> claims, String... roles) {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer(KEYCLOAK_REALM)
                .subject("subject-" + username)
                .claim("preferred_username", username)
                .claims(c -> c.putAll(claims))
                .build();
        var authorities = Stream.of(roles)
                .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                .toList();
        var oidcUser = new DefaultOidcUser(authorities, idToken, "preferred_username");
        return app.newUser(new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "keycloak"));
    }

    /**
     * A user logged in through Keycloak with their first and last name in the ID token.
     */
    protected BrowserlessUserContext keycloakUser(String username, String firstName, String lastName, String... roles) {
        return keycloakUser(username, Map.of("given_name", firstName, "family_name", lastName), roles);
    }
}
