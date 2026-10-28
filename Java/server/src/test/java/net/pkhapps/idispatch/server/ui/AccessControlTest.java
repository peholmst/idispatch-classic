package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.router.RouteNotFoundError;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import net.pkhapps.idispatch.server.ui.management.MunicipalityManagementView;
import net.pkhapps.idispatch.server.ui.reports.ReportsView;
import org.junit.jupiter.api.Test;
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
    void anonymousUser_isRedirectedToLogin() {
        assertThat(getCurrentView()).isInstanceOf(LoginView.class);
        navigate("dws", LoginView.class);
        navigate("reports", LoginView.class);
        navigate("management/municipality", LoginView.class);
        navigate("changePassword", LoginView.class);
    }

    @Test
    @WithAnonymousUser
    void loginView_showsLoginFormWithoutForgotPassword() {
        navigate(LoginView.class);
        var form = find(LoginForm.class).single();
        assertThat(form.getAction()).isEqualTo("login");
        assertThat(form.isForgotPasswordButtonVisible()).isFalse();
        assertThat(form.isError()).isFalse();
    }

    @Test
    @WithAnonymousUser
    void loginView_showsErrorAfterFailedLogin() {
        navigate("login?error", LoginView.class);
        assertThat(find(LoginForm.class).single().isError()).isTrue();
    }

    @Test
    @WithMockUser(username = "plain-user")
    void userWithoutRoles_seesHomeAndChangePasswordOnly() {
        var root = navigate(RootView.class);
        assertThat(find(H1.class, root).single().getText()).isEqualTo("Welcome to iDispatch!");
        assertThat(navigationTabs()).containsExactly("Home", "Change Password");
        assertThat(navbarTitle()).isEqualTo("Home");
    }

    @Test
    @WithMockUser(username = "plain-user")
    void userWithoutRoles_cannotOpenRestrictedViews() {
        navigate("dws", RouteNotFoundError.class);
        navigate("reports", RouteNotFoundError.class);
        navigate("management/user", RouteNotFoundError.class);
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
        assertThat(navigationTabs()).containsExactly("Home", "Dispatcher", "Change Password");
    }

    @Test
    @WithMockUser(username = "report-reader", roles = "REPORT_READER")
    void reportReader_seesReportsEntryAndCanOpenReports() {
        navigate(RootView.class);
        assertThat(navigationTabs()).containsExactly("Home", "Reports", "Change Password");
        navigate(ReportsView.class);
        assertThat(navbarTitle()).isEqualTo("Reports");
    }

    @Test
    @WithMockUser(username = "admin-only", roles = "ADMIN")
    void admin_seesManagementEntryAndCanOpenManagement() {
        navigate(RootView.class);
        assertThat(navigationTabs()).containsExactly("Home", "Management", "Change Password");
        navigate(MunicipalityManagementView.class);
        assertThat(navbarTitle()).isEqualTo("Management Console");
    }

    @Test
    @WithMockUser(username = "all-roles", roles = {"ADMIN", "DISPATCHER", "REPORT_READER"})
    void userWithAllRoles_seesAllEntries() {
        navigate(RootView.class);
        assertThat(navigationTabs())
                .containsExactly("Home", "Dispatcher", "Reports", "Management", "Change Password");
        navigate(DispatchView.class);
        assertThat(navbarTitle()).isEqualTo("Dispatcher");
    }

    @Test
    @WithMockUser(username = "drawer-user")
    void drawer_showsNameOfLoggedInUser() {
        navigate(RootView.class);
        assertThat(find(Span.class).withText("drawer-user").exists()).isTrue();
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
