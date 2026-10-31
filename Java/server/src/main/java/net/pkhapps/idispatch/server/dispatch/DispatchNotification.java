package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.resource.ResourceId;

import java.time.Instant;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * The notification sent when resources are dispatched to an assignment. It keeps the assignment's address, urgency and
 * description as they were when it was sent, and refers to the type and municipality, whose names each destination
 * looks up in its own language.
 *
 * @param assignedResources every resource on the assignment when it was sent, not only those dispatched
 * @param destinations      the destinations of the dispatched resources
 */
public record DispatchNotification(DispatchNotificationId id, Instant timestamp, AssignmentId assignmentId,
                                   MunicipalityId municipalityId, String address, AssignmentTypeId assignmentTypeId,
                                   AssignmentUrgency urgency, String description, Set<ResourceId> assignedResources,
                                   Set<DestinationId> destinations) {

    public DispatchNotification {
        requireNonNull(id);
        requireNonNull(timestamp);
        requireNonNull(assignmentId);
        requireNonNull(municipalityId);
        requireNonNull(address);
        requireNonNull(assignmentTypeId);
        requireNonNull(urgency);
        requireNonNull(description);
        assignedResources = Set.copyOf(assignedResources);
        destinations = Set.copyOf(destinations);
    }

    /**
     * Creates the notification for an assignment that is ready for dispatch.
     *
     * @throws IllegalArgumentException if the assignment has no municipality or type
     */
    public static DispatchNotification of(DispatchNotificationId id, Instant timestamp, Assignment assignment,
                                          Set<ResourceId> assignedResources, Set<DestinationId> destinations) {
        if (assignment.municipalityId() == null || assignment.typeId() == null) {
            throw new IllegalArgumentException("The assignment has no municipality or type");
        }
        return new DispatchNotification(id, timestamp, assignment.id(), assignment.municipalityId(),
                assignment.address(), assignment.typeId(), assignment.urgency(), assignment.description(),
                assignedResources, destinations);
    }
}
