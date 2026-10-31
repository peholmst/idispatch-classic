package net.pkhapps.idispatch.server.workstation;

import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The user interface is in the language of the user's Keycloak account, Finnish, Swedish or English, and in English
 * when the account has none of them. The stories look at the dispatcher view.
 */
class LanguageTest extends UseCaseTest {

    @Test
    void finnishSpeakingDispatcherWorksInFinnish() {
        var resource = given.resource(unique("CS"), ResourceState.ON_SCENE);

        var dispatcher = new Dispatcher(keycloakUser("fi-dispatcher", Map.of("locale", "fi"), "DISPATCHER"),
                DispatcherPage.Language.FINNISH);

        assertThat(dispatcher.viewTitle()).isEqualTo("Hätäkeskus");
        assertThat(dispatcher.seesButton("Uusi tehtävä")).isTrue();
        assertThat(dispatcher.stateOf(resource.callSign())).isEqualTo("Kohteessa");
    }

    @Test
    void swedishSpeakingDispatcherWorksInSwedish() {
        var resource = given.resource(unique("CS"), ResourceState.ON_SCENE);

        var dispatcher = new Dispatcher(keycloakUser("sv-dispatcher", Map.of("locale", "sv-FI"), "DISPATCHER"),
                DispatcherPage.Language.SWEDISH);

        assertThat(dispatcher.viewTitle()).isEqualTo("Larmcentral");
        assertThat(dispatcher.seesButton("Nytt uppdrag")).isTrue();
        assertThat(dispatcher.stateOf(resource.callSign())).isEqualTo("På plats");
    }

    @Test
    void dispatcherWithAnotherLanguageWorksInEnglish() {
        var resource = given.resource(unique("CS"), ResourceState.ON_SCENE);

        var dispatcher = new Dispatcher(keycloakUser("de-dispatcher", Map.of("locale", "de"), "DISPATCHER"));

        assertThat(dispatcher.viewTitle()).isEqualTo("Dispatcher");
        assertThat(dispatcher.seesButton("New Assignment")).isTrue();
        assertThat(dispatcher.stateOf(resource.callSign())).isEqualTo("On scene");
    }
}
