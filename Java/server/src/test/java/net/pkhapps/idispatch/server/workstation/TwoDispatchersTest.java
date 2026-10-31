package net.pkhapps.idispatch.server.workstation;

import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusChanged;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.ui.RunAs;
import net.pkhapps.idispatch.server.ui.TestData.GivenResource;
import net.pkhapps.idispatch.server.ui.UseCaseTest;
import org.junit.jupiter.api.Test;

import static net.pkhapps.idispatch.server.ui.TestData.unique;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two dispatchers work at the same time, each logged in on their own workstation. What one of them does shows up on
 * the other's screen without reloading.
 * <p>
 * The browserless application holds the lock of every session, so these stories cannot catch a view that updates
 * another session's UI without locking it (vaadin/browserless-test#295). For the same reason, {@code OwnerUpdater}
 * updates the other dispatcher's view directly instead of through {@code UI.access}, with the wrong session current,
 * so a story in which both dispatchers have an assignment open fails on the signals of the assignment form.
 */
class TwoDispatchersTest extends UseCaseTest {

    @Test
    void assignmentOpenedByOneDispatcherAppearsForTheOther() {
        var alice = new Dispatcher(user("alice", "DISPATCHER"));
        var bob = new Dispatcher(user("bob", "DISPATCHER"));

        var number = alice.opensANewAssignment();
        bob.receivesUpdates();

        assertThat(bob.seesAssignment(number)).isTrue();
    }

    @Test
    void resourceReservedByOneDispatcherShowsAsReservedForTheOther() {
        var resource = given.resource(unique("CS"), ResourceState.AVAILABLE);
        var alice = new Dispatcher(user("alice", "DISPATCHER"));
        var bob = new Dispatcher(user("bob", "DISPATCHER"));

        alice.opensANewAssignment();
        alice.addsResource(resource.callSign());
        bob.receivesUpdates();

        assertThat(bob.stateOf(resource.callSign())).isEqualTo("Reserved");
    }

    @Test
    void stateChangeMadeWhileAnAdministratorIsActiveStillReachesTheDispatcher() {
        var resource = given.resource(unique("CS"), ResourceState.AT_STATION);
        var status = currentStatusOf(resource);
        var bob = new Dispatcher(user("bob", "DISPATCHER"));
        var carol = user("carol", "ADMIN").newWindow();

        // The change is announced while an administrator, who may not use the dispatcher services, is active, as when
        // a change made in the administration views reaches the dispatchers
        carol.activate();
        spring.publishEvent(new ResourceStatusChanged(this, status));
        bob.receivesUpdates();

        assertThat(bob.stateOf(resource.callSign())).isEqualTo("At station");
    }

    /**
     * The status as the event carries it, which the resource service still holds as an entity.
     */
    private ResourceStatus currentStatusOf(GivenResource resource) {
        var resourceStatusService = spring.getBean(ResourceStatusService.class);
        return RunAs.dispatcher(() -> resourceStatusService.getStatusOfAllResources().stream()
                .filter(status -> status.resourceId().value() == resource.id())
                .findFirst()
                .orElseThrow());
    }
}
