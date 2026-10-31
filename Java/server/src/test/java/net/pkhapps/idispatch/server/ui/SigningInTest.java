package net.pkhapps.idispatch.server.ui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Users log in through Keycloak. Who they are shows at the bottom of the drawer, and Keycloak users find their way to
 * the Keycloak account console from there.
 */
class SigningInTest extends UseCaseTest {

    @Test
    void visitorWhoIsNotLoggedInIsSentToKeycloak() {
        for (var address : List.of("", "dispatcher", "reports", "management/municipality")) {
            var visitor = new AppShell(visitor().newWindow());

            visitor.openAddress(address);

            assertThat(visitor.showsAView()).as("A view is shown at /" + address).isFalse();
            assertThat(visitor.externalAddress()).as("Sent to the login from /" + address)
                    .contains("/oauth2/authorization/keycloak");
        }
    }

    @Test
    void drawerShowsTheUsernameOfAUserWithoutAnIdToken() {
        var user = new AppShell(user("drawer-user").newWindow());

        user.openHome();

        assertThat(user.viewTitle()).contains("Home");
        assertThat(user.userName()).isEqualTo("drawer-user");
    }

    @Test
    void drawerShowsTheFullNameOfAKeycloakUser() {
        var dana = new AppShell(keycloakUser("oidc-user", "Dana", "Dispatcher", "DISPATCHER").newWindow());

        dana.openHome();

        assertThat(dana.userName()).isEqualTo("Dana Dispatcher");
        assertThat(dana.showsText("oidc-user")).isFalse();
    }

    @Test
    void drawerShowsTheUsernameOfAKeycloakUserWithoutAName() {
        var user = new AppShell(keycloakUser("oidc-user", Map.of(), "DISPATCHER").newWindow());

        user.openHome();

        assertThat(user.userName()).isEqualTo("oidc-user");
    }

    @Test
    void keycloakUserCanOpenTheirAccountConsoleInANewTab() {
        var user = new AppShell(keycloakUser("oidc-user", Map.of(), "DISPATCHER").newWindow());

        user.openHome();

        assertThat(user.navigationItems()).containsExactly("Home", "Dispatcher", "My Account", "Log Out");
        assertThat(user.linkOf("My Account")).isEqualTo(KEYCLOAK_REALM + "/account");
        assertThat(user.opensInNewTab("My Account")).isTrue();
    }
}
