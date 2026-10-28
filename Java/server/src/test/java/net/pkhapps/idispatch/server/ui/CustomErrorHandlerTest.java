package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the session error handler is installed and how it reports errors to the user.
 */
class CustomErrorHandlerTest extends AbstractUITest {

    @Test
    @WithMockUser(username = "error-user")
    void sessionErrorHandler_isCustomErrorHandler() {
        navigate(RootView.class);
        assertThat(VaadinSession.getCurrent().getErrorHandler()).isInstanceOf(CustomErrorHandler.class);
    }

    @Test
    @WithMockUser(username = "error-user")
    void accessDenied_showsAccessDeniedNotificationWithReason() {
        navigate(RootView.class);

        VaadinSession.getCurrent().getErrorHandler()
                .error(new ErrorEvent(new AccessDeniedException("You cannot make changes to your own account")));

        assertThat(notificationTexts()).singleElement().satisfies(text -> assertThat(text)
                .contains("Access has been denied")
                .contains("You cannot make changes to your own account"));
    }

    @Test
    @WithMockUser(username = "error-user")
    void unexpectedError_showsNotificationWithErrorId() {
        navigate(RootView.class);

        VaadinSession.getCurrent().getErrorHandler().error(new ErrorEvent(new IllegalStateException("Boom")));
        roundTrip();

        assertThat(notificationTexts()).singleElement().satisfies(text -> assertThat(text)
                .contains("An unexpected error has occurred")
                .contains("Please contact the administrator and give them this error ID:")
                .doesNotContain("Boom")
                .containsPattern("([0-9A-F]{2}:){7}[0-9A-F]{2}"));
    }
}
