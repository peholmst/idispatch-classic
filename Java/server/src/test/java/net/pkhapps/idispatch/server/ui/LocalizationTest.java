package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import net.pkhapps.idispatch.server.entity.ResourceState;
import net.pkhapps.idispatch.server.ui.dws.DispatchView;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the user interface is shown in the language of the UI, which comes from the user's ID token. The
 * test UI is created, and has shown the root layout, before the test sets its language, so only the view is checked.
 */
@WithMockUser(username = "dispatcher", roles = "DISPATCHER")
class LocalizationTest extends AbstractUITest {

    @Test
    void finnish() {
        UI.getCurrent().setLocale(UserLocale.FINNISH);
        navigate(DispatchView.class);

        assertThat(find(H2.class).single().getText()).isEqualTo("Hätäkeskus");
        assertThat(button("Uusi tehtävä").isVisible()).isTrue();
        assertThat(Formatters.formatResourceState(ResourceState.ON_SCENE)).isEqualTo("Kohteessa");
    }

    @Test
    void swedish() {
        UI.getCurrent().setLocale(UserLocale.SWEDISH);
        navigate(DispatchView.class);

        assertThat(find(H2.class).single().getText()).isEqualTo("Larmcentral");
        assertThat(button("Nytt uppdrag").isVisible()).isTrue();
        assertThat(Formatters.formatResourceState(ResourceState.ON_SCENE)).isEqualTo("På plats");
    }

    @Test
    void english_isTheDefault() {
        navigate(DispatchView.class);

        assertThat(UI.getCurrent().getLocale()).isEqualTo(UserLocale.ENGLISH);
        assertThat(button("New Assignment").isVisible()).isTrue();
    }
}
