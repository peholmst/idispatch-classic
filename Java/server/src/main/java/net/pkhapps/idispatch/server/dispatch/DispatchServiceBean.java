package net.pkhapps.idispatch.server.dispatch;

import net.pkhapps.idispatch.server.assignment.Assignment;
import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.resource.ResourceId;
import net.pkhapps.idispatch.server.resource.ResourceListItem;
import net.pkhapps.idispatch.server.resource.ResourceState;
import net.pkhapps.idispatch.server.resource.ResourceStatus;
import net.pkhapps.idispatch.server.resource.ResourceStatusService;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;

@Service
@Secured(Roles.ROLE_DISPATCHER)
class DispatchServiceBean extends AbstractServiceBean implements DispatchService {

    private final DispatchNotificationRepository dispatchNotificationRepository;
    private final DestinationRepository destinationRepository;
    private final ReceiptRepository receiptRepository;
    private final ResourceStatusService resourceStatusService;

    DispatchServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, DispatchNotificationRepository dispatchNotificationRepository, DestinationRepository destinationRepository, ReceiptRepository receiptRepository, ResourceStatusService resourceStatusService) {
        super(applicationContext, txManager);
        this.dispatchNotificationRepository = dispatchNotificationRepository;
        this.destinationRepository = destinationRepository;
        this.receiptRepository = receiptRepository;
        this.resourceStatusService = resourceStatusService;
    }

    @Override
    public DispatchNotification dispatchSelectedResources(Assignment assignment, Collection<ResourceId> resources) throws AssignmentNotReadyForDispatchException {
        var missing = assignment.missingForDispatch();
        if (!missing.isEmpty()) {
            throw new AssignmentNotReadyForDispatchException(missing);
        }
        final Collection<ResourceId> assignedResources = newHashSet(findAssignedResources(assignment));
        final Collection<ResourceId> resourcesToDispatch = resources.stream()
                .filter(assignedResources::contains)
                .collect(Collectors.toList());
        final Collection<Destination> destinations = findDestinations(resourcesToDispatch);
        logger.debug("Dispatching resources {} to assignment {} using destinations {}", resourcesToDispatch, assignment, destinations);
        final DispatchNotification notification = getTxTemplate().execute(status ->
                dispatchNotificationRepository.saveAndFlush(new DispatchNotification(assignment, assignedResources, destinations))
        );
        getApplicationContext().publishEvent(new DispatchNotificationSent(this, notification));
        resourcesToDispatch.forEach(resource -> resourceStatusService.setResourceState(resource, ResourceState.DISPATCHED));
        return notification;
    }

    @Override
    public DispatchNotification dispatchAllResources(Assignment assignment) throws AssignmentNotReadyForDispatchException {
        return dispatchSelectedResources(assignment, findAssignedResources(assignment));
    }

    @Override
    public DispatchNotification dispatchAllReservedResources(Assignment assignment) throws AssignmentNotReadyForDispatchException {
        return dispatchSelectedResources(assignment, resourceStatusService.getStatusOfResourcesAssignedToAssignment(assignment.id())
                .stream()
                .filter(status -> status.getState() == ResourceState.RESERVED)
                .map(ResourceStatus::getResourceId)
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<DispatchNotification> findDispatchNotificationsForAssignment(AssignmentId assignment) {
        logger.trace("Looking up dispatch notifications for assignment {}", assignment);
        return dispatchNotificationRepository.findByAssignmentId(assignment.value());
    }

    @Override
    public List<Receipt> findReceiptsForDispatchNotification(DispatchNotification dispatchNotification) {
        logger.debug("Looking up receipts for dispatch notification {}", dispatchNotification);
        return receiptRepository.findByNotification(dispatchNotification);
    }

    private Collection<Destination> findDestinations(Collection<ResourceId> resources) {
        return resources.stream()
                .flatMap(resource -> destinationRepository.findDestinationsForResource(resource.value()).stream())
                .collect(Collectors.toSet());
    }

    private List<ResourceId> findAssignedResources(Assignment assignment) {
        return resourceStatusService.getResourcesAssignedToAssignment(assignment.id()).stream()
                .map(ResourceListItem::id).toList();
    }
}
