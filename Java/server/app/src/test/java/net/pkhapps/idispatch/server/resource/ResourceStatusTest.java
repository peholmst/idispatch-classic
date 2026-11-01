package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules of a resource's status that the dispatchers' stories rely on: which states follow which, and when a
 * resource counts as available and as on its assignment.
 */
class ResourceStatusTest {

    private static final AssignmentId ASSIGNMENT = new AssignmentId(1);
    private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

    @Test
    void reservingAndDispatchingAreNotOfferedByHand() {
        var available = status(ResourceState.AVAILABLE, null);

        assertThat(available.allValidNextStates())
                .containsExactlyInAnyOrder(ResourceState.AT_STATION, ResourceState.OUT_OF_SERVICE,
                        ResourceState.RESERVED);
        assertThat(available.manualValidNextStates())
                .containsExactlyInAnyOrder(ResourceState.AT_STATION, ResourceState.OUT_OF_SERVICE);
    }

    @Test
    void resourceOnScene_canOnlyBecomeAvailableOrGoOutOfService() {
        assertThat(status(ResourceState.ON_SCENE, ASSIGNMENT).allValidNextStates())
                .containsExactlyInAnyOrder(ResourceState.AVAILABLE, ResourceState.OUT_OF_SERVICE);
    }

    @Test
    void resourceIsAssigned_whileOnItsAssignment() {
        assertThat(status(ResourceState.RESERVED, ASSIGNMENT).isAssigned()).isTrue();
        assertThat(status(ResourceState.ON_SCENE, ASSIGNMENT).isAssigned()).isTrue();
    }

    @Test
    void resourceIsNoLongerAssigned_onceAvailableOrOutOfService_thoughItStillRefersToTheAssignment() {
        var availableAgain = status(ResourceState.AVAILABLE, ASSIGNMENT);
        var outOfService = status(ResourceState.OUT_OF_SERVICE, ASSIGNMENT);

        assertThat(availableAgain.isAssigned()).isFalse();
        assertThat(availableAgain.assignment()).isEqualTo(ASSIGNMENT);
        assertThat(outOfService.isAssigned()).isFalse();
        assertThat(status(ResourceState.RESERVED, null).isAssigned()).as("No assignment").isFalse();
    }

    @Test
    void onlyResourcesAvailableOrAtStation_areAvailable() {
        assertThat(status(ResourceState.AVAILABLE, null).isAvailable()).isTrue();
        assertThat(status(ResourceState.AT_STATION, null).isAvailable()).isTrue();
        assertThat(status(ResourceState.OUT_OF_SERVICE, null).isAvailable()).isFalse();
        assertThat(status(ResourceState.DISPATCHED, ASSIGNMENT).isAvailable()).isFalse();
    }

    @Test
    void changesKeepTheVersion_andArchiveThePreviousState() {
        var before = status(ResourceState.AVAILABLE, null);
        var later = NOW.plusSeconds(60);

        var reserved = before.withState(ResourceState.RESERVED, later).withAssignment(ASSIGNMENT, later);

        assertThat(reserved.version()).isEqualTo(before.version());
        assertThat(reserved.timestamp()).isEqualTo(later);
        assertThat(before.toArchived())
                .isEqualTo(new ArchivedResourceStatus(before.resourceId(), ResourceState.AVAILABLE, null, NOW));
    }

    private static ResourceStatus status(ResourceState state, AssignmentId assignment) {
        return new ResourceStatus(new ResourceId(1), 3, state, assignment, NOW);
    }
}
