package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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

        assertThat(find(Tabs.class).single().getSelectedIndex()).isNotEqualTo(logOutTabIndex());
        verify(authenticationContext, never()).logout();
    }

    @Test
    void selectingLogOut_logsOut() {
        // The real logout needs a servlet response that the browserless mocks don't fully implement
        doNothing().when(authenticationContext).logout();
        navigate(RootView.class);

        test(find(Tabs.class).single()).select(logOutTabIndex());

        verify(authenticationContext).logout();
    }

    private int logOutTabIndex() {
        var labels = find(Tab.class).all().stream().map(tab -> tab.getElement().getTextRecursively()).toList();
        return labels.indexOf("Log Out");
    }
}
