package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

import static java.util.Objects.requireNonNull;

/**
 * A past state of a {@link Resource}, kept for the reports.
 *
 * @param assignment the assignment the resource was on or had last been on, or {@code null} if none
 * @param timestamp  when the resource got this state or assignment
 */
public record ArchivedResourceStatus(ResourceId resourceId, ResourceState state, @Nullable AssignmentId assignment,
                                     Instant timestamp) {

    public ArchivedResourceStatus {
        requireNonNull(resourceId);
        requireNonNull(state);
        requireNonNull(timestamp);
    }
}
