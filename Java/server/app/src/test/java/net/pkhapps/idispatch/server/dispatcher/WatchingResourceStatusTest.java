package net.pkhapps.idispatch.server.dispatcher;

import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.ui.RunAs;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * A dispatcher keeps an eye on every resource on the resource status view, a card per resource, while the resources
 * change state.
 */
class WatchingResourceStatusTest extends UseCaseTest {

    @Test
    void dispatcherSeesACardPerResourceInCallSignOrder() {
        var prefix = unique("RS");
        given.resource(prefix + "-C", ResourceState.OUT_OF_SERVICE);
        given.resource(prefix + "-A", ResourceState.AT_STATION);
        given.resource(prefix + "-B", ResourceState.AVAILABLE);
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));

        dispatcher.opensResourceStatus();
        dispatcher.filtersResourceStatusByCallSign(prefix.toLowerCase());

        assertThat(dispatcher.resourceCards()).containsExactly(prefix + "-A", prefix + "-B", prefix + "-C");
        assertThat(dispatcher.stateOnCardOf(prefix + "-A")).isEqualTo("At station");
        assertThat(dispatcher.stateOnCardOf(prefix + "-C")).isEqualTo("Out of service");
        assertThat(dispatcher.assignmentOnCardOf(prefix + "-A")).isEmpty();
    }

    @Test
    void dispatcherFiltersTheCardsByState() {
        var prefix = unique("RS");
        given.resource(prefix + "-A", ResourceState.AT_STATION);
        given.resource(prefix + "-B", ResourceState.AVAILABLE);
        given.resource(prefix + "-C", ResourceState.OUT_OF_SERVICE);
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));
        dispatcher.opensResourceStatus();
        dispatcher.filtersResourceStatusByCallSign(prefix);

        dispatcher.filtersResourceStatusByStates("Available", "Out of service");

        assertThat(dispatcher.resourceCards()).containsExactly(prefix + "-B", prefix + "-C");

        dispatcher.clearsTheStateFilter();

        assertThat(dispatcher.resourceCards()).containsExactly(prefix + "-A", prefix + "-B", prefix + "-C");
    }

    @Test
    void dispatcherIsToldWhenNoResourceMatchesTheFilters() {
        var dispatcher = new Dispatcher(user("dispatcher", "DISPATCHER"));
        dispatcher.opensResourceStatus();

        dispatcher.filtersResourceStatusByCallSign(unique("NOTHING"));

        assertThat(dispatcher.resourceCards()).isEmpty();
        assertThat(dispatcher.isToldNoResourcesMatch()).isTrue();
    }

    @Test
    void stateChangeMadeByAnotherDispatcherShowsOnTheCardRightAway() {
        var resource = given.resource(unique("RS"), ResourceState.AVAILABLE);
        var watcher = new Dispatcher(user("alice", "DISPATCHER"));
        watcher.opensResourceStatus();
        watcher.filtersResourceStatusByCallSign(resource.callSign());
        var bob = new Dispatcher(user("bob", "DISPATCHER"));

        var number = bob.opensANewAssignment();
        bob.addsResource(resource.callSign());
        watcher.receivesUpdates();

        assertThat(watcher.stateOnCardOf(resource.callSign())).isEqualTo("Reserved");
        assertThat(watcher.assignmentOnCardOf(resource.callSign())).contains("(" + number + ")");

        bob.removesResource(resource.callSign());
        watcher.receivesUpdates();

        assertThat(watcher.stateOnCardOf(resource.callSign())).isEqualTo("Available");
        assertThat(watcher.assignmentOnCardOf(resource.callSign())).isEmpty();
    }

    @Test
    void stateChangeMadeFromTheUnitsMobileAppShowsOnTheCardRightAway() {
        var resource = given.resource(unique("RS"), ResourceState.AT_STATION);
        var watcher = new Dispatcher(user("alice", "DISPATCHER"));
        watcher.opensResourceStatus();
        watcher.filtersResourceStatusByCallSign(resource.callSign());

        RunAs.unit(resource.id(), () -> spring.getBean(ResourceStatusService.class)
                .setResourceStateManually(new ResourceId(resource.id()), ResourceState.OUT_OF_SERVICE));
        watcher.receivesUpdates();

        assertThat(watcher.stateOnCardOf(resource.callSign())).isEqualTo("Out of service");
    }
}
