package net.pkhapps.idispatch.server.dispatch;


import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceState;

import java.util.Collection;
import java.util.List;

/**
 * Service interface for dispatch operations.
 */
public interface DispatchService {

    /**
     * Dispatches the selected resources to the assignment and fires a {@link DispatchNotificationSent} event. Resources
     * in the {@link ResourceState#RESERVED} state will change into the {@link ResourceState#DISPATCHED} state, any
     * other resources will remain in their previous states. Any resources not assigned to the assignment will be
     * ignored.
     * <p>
     * Callers can listen for {@link DispatchNotificationReceived} events to check whether the dispatch notification
     * reached its destinations.
     *
     * @throws AssignmentNotReadyForDispatchException if the assignment did not contain enough information
     */
    DispatchNotification dispatchSelectedResources(Assignment assignment, Collection<ResourceId> resources) throws AssignmentNotReadyForDispatchException;

    /**
     * Same as calling {@link #dispatchSelectedResources(Assignment, Collection)}, but passing in all assigned
     * resources.
     *
     * @throws AssignmentNotReadyForDispatchException if the assignment did not contain enough information
     * @see net.pkhapps.idispatch.server.resource.ResourceStatus#isAssigned()
     */
    DispatchNotification dispatchAllResources(Assignment assignment) throws AssignmentNotReadyForDispatchException;

    /**
     * Same as calling {@link #dispatchSelectedResources(Assignment, Collection)}}, but passing in all assigned
     * resources that are in the {@link ResourceState#RESERVED} state.
     *
     * @throws AssignmentNotReadyForDispatchException if the assignment did not contain enough information
     * @see net.pkhapps.idispatch.server.resource.ResourceStatus#isAssigned()
     */
    DispatchNotification dispatchAllReservedResources(Assignment assignment) throws AssignmentNotReadyForDispatchException;

    /**
     * Finds all dispatch notifications for the specified assignment.
     */
    List<DispatchNotification> findDispatchNotificationsForAssignment(AssignmentId assignment);

    /**
     * Finds all receipts for the specified dispatch notification.
     */
    List<Receipt> findReceiptsForDispatchNotification(DispatchNotificationId dispatchNotification);
}
