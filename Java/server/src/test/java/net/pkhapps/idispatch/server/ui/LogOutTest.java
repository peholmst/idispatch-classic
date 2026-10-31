package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Users log out from the drawer. Browserless tests don't run Spring Security's logout handlers, so these stories end
 * where the application hands over to {@link AuthenticationContext#logout()}; that the user then lands on Keycloak's
 * logout is left to a test in a real browser.
 */
class LogOutTest extends UseCaseTest {

    @MockitoSpyBean
    AuthenticationContext authenticationContext;

    @Test
    void userLogsOutFromTheDrawer() {
        // The real logout needs a servlet response that the browserless mocks don't fully implement
        doNothing().when(authenticationContext).logout();
        var user = new AppShell(user("logout-user").newWindow());
        user.openHome();

        user.click("Log Out");

        verify(authenticationContext).logout();
    }

    @Test
    void openingTheApplicationDoesNotLogOut() {
        var user = new AppShell(user("logout-user").newWindow());

        user.openHome();

        verify(authenticationContext, never()).logout();
    }

    @Test
    void logOutIsAnActionNotALinkToFollow() {
        var user = new AppShell(user("logout-user").newWindow());

        user.openHome();

        assertThat(user.followsLink("Log Out")).isFalse();
    }
}
