package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractService;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lets dispatchers send the resources on an assignment out, and finds the notifications and receipts of what was
 * sent.
 */
@Service
@PreAuthorize(Roles.HAS_DISPATCHER)
public class DispatchService extends AbstractService {

    private final DispatchNotificationRepository dispatchNotificationRepository;
    private final DestinationRepository destinationRepository;
    private final ResourceStatusService resourceStatusService;

    DispatchService(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                    DispatchNotificationRepository dispatchNotificationRepository,
                    DestinationRepository destinationRepository, ResourceStatusService resourceStatusService) {
        super(applicationContext, txManager);
        this.dispatchNotificationRepository = dispatchNotificationRepository;
        this.destinationRepository = destinationRepository;
        this.resourceStatusService = resourceStatusService;
    }

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
    public DispatchNotification dispatchSelectedResources(Assignment assignment, Collection<ResourceId> resources) throws AssignmentNotReadyForDispatchException {
        var missing = assignment.missingForDispatch();
        if (!missing.isEmpty()) {
            throw new AssignmentNotReadyForDispatchException(missing);
        }
        final Set<ResourceId> assignedResources = new LinkedHashSet<>(findAssignedResources(assignment));
        final List<ResourceId> resourcesToDispatch = resources.stream()
                .filter(assignedResources::contains)
                .toList();
        logger.debug("Dispatching resources {} to assignment {}", resourcesToDispatch, assignment.id());
        record Sent(DispatchNotification notification, List<Destination> destinations) {
        }
        var sent = getTxTemplate().execute(status -> {
            var destinations = resourcesToDispatch.isEmpty() ? List.<Destination>of()
                    : destinationRepository.findActiveFor(resourcesToDispatch);
            var notification = dispatchNotificationRepository.insert(assignment, assignedResources,
                    destinations.stream().map(Destination::id).collect(Collectors.toSet()));
            return new Sent(notification, destinations);
        });
        logger.debug("Sent notification {} to destinations {}", sent.notification().id(),
                sent.destinations().stream().map(Destination::id).toList());
        getApplicationContext().publishEvent(new DispatchNotificationSent(this, sent.notification(), sent.destinations()));
        resourcesToDispatch.forEach(resource -> resourceStatusService.setResourceState(resource, ResourceState.DISPATCHED));
        return sent.notification();
    }

    /**
     * Same as calling {@link #dispatchSelectedResources(Assignment, Collection)}, but passing in all assigned
     * resources.
     *
     * @throws AssignmentNotReadyForDispatchException if the assignment did not contain enough information
     * @see net.pkhapps.idispatch.server.resource.ResourceStatus#isAssigned()
     */
    public DispatchNotification dispatchAllResources(Assignment assignment) throws AssignmentNotReadyForDispatchException {
        return dispatchSelectedResources(assignment, findAssignedResources(assignment));
    }

    /**
     * Same as calling {@link #dispatchSelectedResources(Assignment, Collection)}}, but passing in all assigned
     * resources that are in the {@link ResourceState#RESERVED} state.
     *
     * @throws AssignmentNotReadyForDispatchException if the assignment did not contain enough information
     * @see net.pkhapps.idispatch.server.resource.ResourceStatus#isAssigned()
     */
    public DispatchNotification dispatchAllReservedResources(Assignment assignment) throws AssignmentNotReadyForDispatchException {
        return dispatchSelectedResources(assignment, resourceStatusService.getStatusOfResourcesAssignedToAssignment(assignment.id())
                .stream()
                .filter(status -> status.state() == ResourceState.RESERVED)
                .map(ResourceStatus::resourceId)
                .toList()
        );
    }

    /**
     * Finds all dispatch notifications for the specified assignment.
     */
    public List<DispatchNotification> findDispatchNotificationsForAssignment(AssignmentId assignment) {
        logger.trace("Looking up dispatch notifications for assignment {}", assignment);
        return dispatchNotificationRepository.findByAssignment(assignment);
    }

    /**
     * Finds all receipts for the specified dispatch notification.
     */
    public List<Receipt> findReceiptsForDispatchNotification(DispatchNotificationId dispatchNotification) {
        logger.debug("Looking up receipts for dispatch notification {}", dispatchNotification);
        return dispatchNotificationRepository.findReceipts(dispatchNotification);
    }

    private List<ResourceId> findAssignedResources(Assignment assignment) {
        return resourceStatusService.getResourcesAssignedToAssignment(assignment.id()).stream()
                .map(ResourceListItem::id).toList();
    }
}
