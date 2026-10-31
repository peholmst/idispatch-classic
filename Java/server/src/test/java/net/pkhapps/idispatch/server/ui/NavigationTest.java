package net.pkhapps.idispatch.server.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each role sees the views it may use in the drawer, and cannot open the others, not even by typing their address.
 */
class NavigationTest extends UseCaseTest {

    @Test
    void userWithoutRolesSeesOnlyHome() {
        var user = new AppShell(user("plain-user").newWindow());

        user.openHome();

        assertThat(user.navigationItems()).containsExactly("Home", "Log Out");
        assertThat(user.viewTitle()).contains("Home");
        assertThat(cannotOpen(user, "dws")).isTrue();
        assertThat(cannotOpen(user, "reports")).isTrue();
    }

    @Test
    void dispatcherSeesOnlyTheDispatcher() {
        var dispatcher = new AppShell(user("dispatcher", "DISPATCHER").newWindow());

        dispatcher.openHome();

        assertThat(dispatcher.navigationItems()).containsExactly("Home", "Dispatcher", "Log Out");
        dispatcher.goTo("Dispatcher");
        assertThat(dispatcher.viewTitle()).contains("Dispatcher");
        assertThat(cannotOpen(dispatcher, "reports")).isTrue();
        assertThat(cannotOpen(dispatcher, "management/municipality")).isTrue();
    }

    @Test
    void reportReaderSeesOnlyReports() {
        reportUserSeesOnlyReports("report-reader", "REPORT_READER");
    }

    @Test
    void reportWriterSeesOnlyReports() {
        reportUserSeesOnlyReports("report-writer", "REPORT_WRITER");
    }

    @Test
    void reportReviewerSeesOnlyReports() {
        reportUserSeesOnlyReports("report-reviewer", "REPORT_REVIEWER");
    }

    @Test
    void administratorSeesOnlyTheAdministrationViews() {
        var admin = new AppShell(user("admin-only", "ADMIN").newWindow());

        admin.openHome();

        assertThat(admin.navigationItems()).containsExactly("Home", "Municipalities", "Assignment Types",
                "Resources", "Resource Types", "Destinations", "Log Out");
        admin.goTo("Municipalities");
        assertThat(admin.viewTitle()).contains("Municipalities");
        assertThat(cannotOpen(admin, "dws")).isTrue();
        assertThat(cannotOpen(admin, "reports")).isTrue();
    }

    @Test
    void userWithEveryRoleSeesEveryView() {
        var user = new AppShell(user("all-roles", "ADMIN", "DISPATCHER", "REPORT_READER").newWindow());

        user.openHome();

        assertThat(user.navigationItems()).containsExactly("Home", "Dispatcher", "Reports", "Municipalities",
                "Assignment Types", "Resources", "Resource Types", "Destinations", "Log Out");
        user.goTo("Dispatcher");
        assertThat(user.viewTitle()).contains("Dispatcher");
    }

    private void reportUserSeesOnlyReports(String username, String role) {
        var user = new AppShell(user(username, role).newWindow());

        user.openHome();

        assertThat(user.navigationItems()).containsExactly("Home", "Reports", "Log Out");
        user.goTo("Reports");
        assertThat(user.viewTitle()).contains("Reports");
        assertThat(cannotOpen(user, "dws")).isTrue();
        assertThat(cannotOpen(user, "management/resource")).isTrue();
    }

    private static boolean cannotOpen(AppShell user, String address) {
        user.openAddress(address);
        return user.showsNotFound();
    }
}
