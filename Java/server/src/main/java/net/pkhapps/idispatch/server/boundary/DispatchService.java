package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.*;
import net.pkhapps.idispatch.server.events.DispatchNotificationReceived;
import net.pkhapps.idispatch.server.events.DispatchNotificationSent;

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
     * @throws ValidationFailedException if the assignment did not contain enough information
     */
    DispatchNotification dispatchSelectedResources(Assignment assignment, Collection<Resource> resources) throws ValidationFailedException;

    /**
     * Same as calling {@link #dispatchSelectedResources(Assignment, Collection)}, but passing in all assigned
     * resources.
     *
     * @throws ValidationFailedException if the assignment did not contain enough information
     * @see ResourceStatus#isAssigned()
     */
    DispatchNotification dispatchAllResources(Assignment assignment) throws ValidationFailedException;

    /**
     * Same as calling {@link #dispatchSelectedResources(Assignment, Collection)}}, but passing in all assigned
     * resources that are in the {@link ResourceState#RESERVED} state.
     *
     * @throws ValidationFailedException if the assignment did not contain enough information
     * @see ResourceStatus#isAssigned()
     */
    DispatchNotification dispatchAllReservedResources(Assignment assignment) throws ValidationFailedException;

    /**
     * Finds all dispatch notifications for the specified assignment.
     */
    List<DispatchNotification> findDispatchNotificationsForAssignment(Assignment assignment);

    /**
     * Finds all receipts for the specified dispatch notification.
     */
    List<Receipt> findReceiptsForDispatchNotification(DispatchNotification dispatchNotification);
}
