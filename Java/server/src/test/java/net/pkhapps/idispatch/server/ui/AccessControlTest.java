package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.sidenav.SideNavItem;
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
import java.util.Map;

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
        assertThat(navigationItems()).containsExactly("Home", "Log Out");
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
        assertThat(navigationItems()).containsExactly("Home", "Dispatcher", "Log Out");
    }

    @Test
    @WithMockUser(username = "report-reader", roles = "REPORT_READER")
    void reportReader_seesReportsEntryAndCanOpenReports() {
        navigate(RootView.class);
        assertThat(navigationItems()).containsExactly("Home", "Reports", "Log Out");
        navigate(ReportsView.class);
        assertThat(navbarTitle()).isEqualTo("Reports");
    }

    @Test
    @WithMockUser(username = "report-writer", roles = "REPORT_WRITER")
    void reportWriter_seesReportsEntryAndCanOpenReports() {
        navigate(RootView.class);
        assertThat(navigationItems()).containsExactly("Home", "Reports", "Log Out");
        navigate(ReportsView.class);
        assertThat(navbarTitle()).isEqualTo("Reports");
        navigate("dws", RouteNotFoundError.class);
    }

    @Test
    @WithMockUser(username = "report-reviewer", roles = "REPORT_REVIEWER")
    void reportReviewer_seesReportsEntryAndCanOpenReports() {
        navigate(RootView.class);
        assertThat(navigationItems()).containsExactly("Home", "Reports", "Log Out");
        navigate(ReportsView.class);
        assertThat(navbarTitle()).isEqualTo("Reports");
        navigate("dws", RouteNotFoundError.class);
    }

    @Test
    @WithMockUser(username = "admin-only", roles = "ADMIN")
    void admin_seesManagementEntryAndCanOpenManagement() {
        navigate(RootView.class);
        assertThat(navigationItems()).containsExactly("Home", "Management", "Log Out");
        navigate(MunicipalityManagementView.class);
        assertThat(navbarTitle()).isEqualTo("Management Console");
    }

    @Test
    @WithMockUser(username = "all-roles", roles = {"ADMIN", "DISPATCHER", "REPORT_READER"})
    void userWithAllRoles_seesAllEntries() {
        navigate(RootView.class);
        assertThat(navigationItems())
                .containsExactly("Home", "Dispatcher", "Reports", "Management", "Log Out");
        navigate(DispatchView.class);
        assertThat(navbarTitle()).isEqualTo("Dispatcher");
    }

    @Test
    @WithMockUser(username = "drawer-user")
    void drawer_showsUsernameWhenThereIsNoIdToken() {
        navigate(RootView.class);
        assertThat(find(Avatar.class).single().getName()).isEqualTo("drawer-user");
        assertThat(find(Span.class).withText("drawer-user").exists()).isTrue();
    }

    @Test
    void keycloakUser_seesAvatarWithFirstAndLastName() {
        loginWithIdToken(Map.of("given_name", "Dana", "family_name", "Dispatcher"));

        navigate(RootView.class);

        assertThat(find(Avatar.class).single().getName()).isEqualTo("Dana Dispatcher");
        assertThat(find(Span.class).withText("Dana Dispatcher").exists()).isTrue();
        assertThat(find(Span.class).withText("oidc-user").exists()).isFalse();
    }

    @Test
    void keycloakUserWithoutName_seesUsername() {
        loginWithIdToken(Map.of());

        navigate(RootView.class);

        assertThat(find(Avatar.class).single().getName()).isEqualTo("oidc-user");
        assertThat(find(Span.class).withText("oidc-user").exists()).isTrue();
    }

    @Test
    void keycloakUser_seesLinkToKeycloakAccountConsole() {
        loginWithIdToken(Map.of());

        navigate(RootView.class);

        assertThat(navigationItems()).containsExactly("Home", "Dispatcher", "My Account", "Log Out");
        var account = find(SideNavItem.class).withCondition(item -> "My Account".equals(item.getLabel())).single();
        assertThat(account.getPath()).isEqualTo("http://keycloak.invalid/realms/idispatch/account");
        assertThat(account.isOpenInNewBrowserTab()).isTrue();
    }

    private void loginWithIdToken(Map<String, Object> nameClaims) {
        var idToken = OidcIdToken.withTokenValue("id-token")
                .issuer("http://keycloak.invalid/realms/idispatch")
                .subject("f2b4c1d0")
                .claim("preferred_username", "oidc-user")
                .claims(claims -> claims.putAll(nameClaims))
                .build();
        var user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority(Roles.ROLE_DISPATCHER)), idToken,
                "preferred_username");
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new OAuth2AuthenticationToken(user, user.getAuthorities(), "keycloak"));
        SecurityContextHolder.setContext(context);
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

    @Test
    @WithMockUser(username = "admin-only", roles = "ADMIN")
    void managementItem_matchesEverySectionOfTheManagementConsole() {
        navigate(RootView.class);

        var management = find(SideNavItem.class).withCondition(item -> "Management".equals(item.getLabel())).single();
        assertThat(management.getPath()).isEqualTo("management/municipality");
        assertThat(management.getPathAliases()).containsExactlyInAnyOrder("management/municipality",
                "management/assignment-type", "management/resource", "management/resource-type",
                "management/destination");
    }

    private List<String> navigationItems() {
        return find(SideNavItem.class).all().stream()
                .map(SideNavItem::getLabel)
                .toList();
    }

    private String navbarTitle() {
        return find(H2.class).single().getText();
    }
}
