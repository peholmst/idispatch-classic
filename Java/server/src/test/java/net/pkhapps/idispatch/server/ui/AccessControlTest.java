package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouteNotFoundError;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.MunicipalityManagementView;
import net.pkhapps.idispatch.server.ui.reports.ReportsView;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies who can reach which view, and which navigation entries each role sees.
 */
class AccessControlTest extends AbstractUITest {

    @Test
    @WithAnonymousUser
    void anonymousUser_isRedirectedToKeycloakLogin() {
        assertThat(keycloakLoginRedirects()).isNotEmpty();
        for (var location : List.of("", "dws", "reports", "management/municipality")) {
            UI.getCurrent().navigate(location);
            assertThat(UI.getCurrent().getInternals().getActiveRouterTargetsChain()).isEmpty();
            assertThat(keycloakLoginRedirects()).isNotEmpty();
        }
    }

    @Test
    @WithMockUser(username = "plain-user")
    void userWithoutRoles_seesHomeOnly() {
        var root = navigate(RootView.class);
        assertThat(find(H1.class, root).single().getText()).isEqualTo("Welcome to iDispatch!");
        assertThat(navigationTabs()).containsExactly("Home");
        assertThat(navbarTitle()).isEqualTo("Home");
    }

    @Test
    @WithMockUser(username = "plain-user")
    void userWithoutRoles_cannotOpenRestrictedViews() {
        navigate("dws", RouteNotFoundError.class);
        navigate("reports", RouteNotFoundError.class);
    }

    @Test
    @WithMockUser(username = "dispatcher", roles = "DISPATCHER")
    void dispatcher_cannotOpenReportsOrManagement() {
        navigate("reports", RouteNotFoundError.class);
        navigate("management/municipality", RouteNotFoundError.class);
    }

    @Test
    @WithMockUser(username = "report-reader", roles = "REPORT_READER")
    void reportReader_cannotOpenDispatcherOrManagement() {
        navigate("dws", RouteNotFoundError.class);
        navigate("management/resource", RouteNotFoundError.class);
    }

    @Test
    @WithMockUser(username = "admin-only", roles = "ADMIN")
    void admin_cannotOpenDispatcherOrReports() {
        navigate("dws", RouteNotFoundError.class);
        navigate("reports", RouteNotFoundError.class);
    }

    @Test
    @WithMockUser(username = "dispatcher", roles = "DISPATCHER")
    void dispatcher_seesDispatcherEntry() {
        navigate(RootView.class);
        assertThat(navigationTabs()).containsExactly("Home", "Dispatcher");
    }

    @Test
    @WithMockUser(username = "report-reader", roles = "REPORT_READER")
    void reportReader_seesReportsEntryAndCanOpenReports() {
        navigate(RootView.class);
        assertThat(navigationTabs()).containsExactly("Home", "Reports");
        navigate(ReportsView.class);
        assertThat(navbarTitle()).isEqualTo("Reports");
    }

    @Test
    @WithMockUser(username = "admin-only", roles = "ADMIN")
    void admin_seesManagementEntryAndCanOpenManagement() {
        navigate(RootView.class);
        assertThat(navigationTabs()).containsExactly("Home", "Management");
        navigate(MunicipalityManagementView.class);
        assertThat(navbarTitle()).isEqualTo("Management Console");
    }

    @Test
    @WithMockUser(username = "all-roles", roles = {"ADMIN", "DISPATCHER", "REPORT_READER"})
    void userWithAllRoles_seesAllEntries() {
        navigate(RootView.class);
        assertThat(navigationTabs())
                .containsExactly("Home", "Dispatcher", "Reports", "Management");
        navigate(DispatchView.class);
        assertThat(navbarTitle()).isEqualTo("Dispatcher");
    }

    @Test
    @WithMockUser(username = "drawer-user")
    void drawer_showsNameOfLoggedInUser() {
        navigate(RootView.class);
        assertThat(find(Span.class).withText("drawer-user").exists()).isTrue();
    }

    @Test
    void keycloakUser_seesNameAndLinkToKeycloakAccountConsole() {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://keycloak.invalid/realms/idispatch")
                .subject("f2b4c1d0")
                .claim("preferred_username", "oidc-user")
                .build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority(Roles.ROLE_DISPATCHER)), idToken,
                "preferred_username");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak"));
        SecurityContextHolder.setContext(context);

        navigate(RootView.class);

        assertThat(find(Span.class).withText("oidc-user").exists()).isTrue();
        assertThat(navigationTabs()).containsExactly("Home", "Dispatcher", "My Account");
        assertThat(find(Anchor.class).single().getHref())
                .isEqualTo("http://keycloak.invalid/realms/idispatch/account");
    }

    /**
     * Returns the pending client-side redirects to the Keycloak login, which Vaadin issues instead of showing a view.
     */
    private List<?> keycloakLoginRedirects() {
        return UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(invocation -> invocation.getInvocation().getParameters())
                .filter(parameters -> parameters.contains("/oauth2/authorization/keycloak"))
                .toList();
    }

    private List<String> navigationTabs() {
        return find(Tab.class).all().stream()
                .map(tab -> tab.getElement().getTextRecursively())
                .toList();
    }

    private String navbarTitle() {
        return find(H2.class).single().getText();
    }
}
