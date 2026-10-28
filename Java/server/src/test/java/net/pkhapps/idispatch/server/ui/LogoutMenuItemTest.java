package net.pkhapps.idispatch.server.ui;

import com.vaadin.browserless.ComponentTester;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies that the Log Out menu item logs out. Browserless tests don't run Spring Security's logout handlers, so this
 * only checks that the menu item hands over to {@link AuthenticationContext#logout()}.
 */
@WithMockUser(username = "logout-user")
class LogoutMenuItemTest extends AbstractUITest {

    @MockitoSpyBean
    AuthenticationContext authenticationContext;

    @Test
    void openingTheMenu_doesNotLogOut() {
        navigate(RootView.class);

        verify(authenticationContext, never()).logout();
    }

    @Test
    void selectingLogOut_logsOut() {
        // The real logout needs a servlet response that the browserless mocks don't fully implement
        doNothing().when(authenticationContext).logout();
        navigate(RootView.class);

        new SideNavItemTester(logOutItem()).clickLink();

        verify(authenticationContext).logout();
    }

    @Test
    void logOutItem_isNotFollowedAsALink() {
        navigate(RootView.class);

        assertThat(logOutItem().isRouterIgnore()).isTrue();
    }

    private SideNavItem logOutItem() {
        return find(SideNavItem.class).withCondition(item -> "Log Out".equals(item.getLabel())).single();
    }

    /**
     * Clicks a side navigation item the way the browser does, firing the DOM event that its listeners receive.
     */
    private static final class SideNavItemTester extends ComponentTester<SideNavItem> {

        SideNavItemTester(SideNavItem item) {
            super(item);
        }

        void clickLink() {
            fireDomEvent("click");
        }
    }
}
