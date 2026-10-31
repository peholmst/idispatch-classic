package net.pkhapps.idispatch.server.ui;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * When something goes wrong, the user is told: why access was denied, or an error ID to give the administrator, who
 * finds the details in the log under that ID.
 */
class ErrorReportingTest extends UseCaseTest {

    @Test
    void userIsToldWhyAccessWasDenied() {
        var user = new AppShell(user("error-user").newWindow());
        user.openHome();

        user.somethingFailsWith(new AccessDeniedException("You cannot make changes to your own account"));

        assertThat(user.notifications()).singleElement().satisfies(text -> assertThat(text)
                .contains("Access has been denied")
                .contains("You cannot make changes to your own account"));
    }

    @Test
    void userGetsAnErrorIdForAnUnexpectedErrorButNotItsDetails() {
        var user = new AppShell(user("error-user").newWindow());
        user.openHome();

        user.somethingFailsWith(new IllegalStateException("Boom"));

        assertThat(user.notifications()).singleElement().satisfies(text -> assertThat(text)
                .contains("An unexpected error has occurred")
                .contains("Please contact the administrator and give them this error ID:")
                .doesNotContain("Boom")
                .containsPattern("([0-9A-F]{2}:){7}[0-9A-F]{2}"));
    }
}
