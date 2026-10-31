package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;

/**
 * The current state of a {@link Resource}, and the assignment it is on or was last on. Every change of state is kept
 * as an {@link ArchivedResourceStatus} of the state before it.
 *
 * @param version    incremented on every change, so that a change based on an older version can be refused
 * @param assignment the assignment the resource is or was last on, or {@code null} if none
 * @param timestamp  when the resource got this state or assignment
 */
public record ResourceStatus(ResourceId resourceId, long version, ResourceState state,
                             @Nullable AssignmentId assignment, Instant timestamp) {

    private static final Map<ResourceState, Set<ResourceState>> TRANSITIONS = Map.of(
            ResourceState.RESERVED, EnumSet.of(ResourceState.DISPATCHED, ResourceState.EN_ROUTE, ResourceState.ON_SCENE,
                    ResourceState.AVAILABLE, ResourceState.OUT_OF_SERVICE, ResourceState.AT_STATION),
            ResourceState.DISPATCHED, EnumSet.of(ResourceState.EN_ROUTE, ResourceState.AVAILABLE,
                    ResourceState.OUT_OF_SERVICE, ResourceState.AT_STATION),
            ResourceState.EN_ROUTE, EnumSet.of(ResourceState.ON_SCENE, ResourceState.AVAILABLE,
                    ResourceState.OUT_OF_SERVICE),
            ResourceState.ON_SCENE, EnumSet.of(ResourceState.AVAILABLE, ResourceState.OUT_OF_SERVICE),
            ResourceState.AVAILABLE, EnumSet.of(ResourceState.AT_STATION, ResourceState.OUT_OF_SERVICE,
                    ResourceState.RESERVED),
            ResourceState.AT_STATION, EnumSet.of(ResourceState.AVAILABLE, ResourceState.OUT_OF_SERVICE,
                    ResourceState.RESERVED),
            ResourceState.OUT_OF_SERVICE, EnumSet.of(ResourceState.AVAILABLE, ResourceState.AT_STATION));

    public ResourceStatus {
        requireNonNull(resourceId);
        requireNonNull(state);
        requireNonNull(timestamp);
    }

    /**
     * Returns the states the resource can go to from its current one.
     */
    public Set<ResourceState> allValidNextStates() {
        return unmodifiableSet(TRANSITIONS.get(state));
    }

    /**
     * Returns the states a dispatcher can set by hand, which leaves out reserving and dispatching.
     */
    public Set<ResourceState> manualValidNextStates() {
        var states = EnumSet.copyOf(TRANSITIONS.get(state));
        states.remove(ResourceState.RESERVED);
        states.remove(ResourceState.DISPATCHED);
        return unmodifiableSet(states);
    }

    /**
     * Returns whether the resource is available for new assignments.
     */
    public boolean isAvailable() {
        return state.isAvailableForNewAssignments();
    }

    /**
     * Returns whether the resource is on its assignment. A resource that is available again or out of service still
     * refers to its last assignment, but is no longer on it.
     */
    public boolean isAssigned() {
        return assignment != null && !state.isAvailableForNewAssignments() && state != ResourceState.OUT_OF_SERVICE;
    }

    /**
     * Returns this status changed to the given state at the given time, to be saved as a change of this version.
     */
    public ResourceStatus withState(ResourceState state, Instant now) {
        return new ResourceStatus(resourceId, version, state, assignment, now);
    }

    /**
     * Returns this status with the given assignment, or none, at the given time, to be saved as a change of this
     * version.
     */
    public ResourceStatus withAssignment(@Nullable AssignmentId assignment, Instant now) {
        return new ResourceStatus(resourceId, version, state, assignment, now);
    }

    /**
     * Returns this status as it is kept once it has been replaced by a newer one.
     */
    public ArchivedResourceStatus toArchived() {
        return new ArchivedResourceStatus(resourceId, state, assignment, timestamp);
    }
}
