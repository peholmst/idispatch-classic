package net.pkhapps.idispatch.server.resource;

import jakarta.persistence.*;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.shared.AbstractTimestampedEntity;
import org.jspecify.annotations.Nullable;

/**
 * Base entity that contains a state of a {@link Resource}.
 */
@MappedSuperclass
public abstract class AbstractResourceStateChange extends AbstractTimestampedEntity {

    public static final String PROP_STATE = "state";
    public static final String PROP_RESOURCE = "resource";
    public static final String PROP_ASSIGNMENT_ID = "assignmentId";

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ResourceState state = ResourceState.OUT_OF_SERVICE;
    @Column(name = "assignment_id")
    private Long assignmentId;

    protected AbstractResourceStateChange() {
    }

    public abstract Resource getResource();

    public ResourceState getState() {
        return state;
    }

    protected void setState(ResourceState state) {
        this.state = state;
    }

    /**
     * Returns the assignment the resource is or was last on, or {@code null} if none.
     */
    public @Nullable AssignmentId getAssignmentId() {
        return assignmentId == null ? null : new AssignmentId(assignmentId);
    }

    protected void setAssignmentId(@Nullable AssignmentId assignmentId) {
        this.assignmentId = assignmentId == null ? null : assignmentId.value();
    }
}
