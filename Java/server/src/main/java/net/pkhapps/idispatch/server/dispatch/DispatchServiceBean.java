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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Secured(Roles.ROLE_DISPATCHER)
class DispatchServiceBean extends AbstractServiceBean implements DispatchService {

    private final DispatchNotificationRepository dispatchNotificationRepository;
    private final DestinationRepository destinationRepository;
    private final ResourceStatusService resourceStatusService;

    DispatchServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                        DispatchNotificationRepository dispatchNotificationRepository,
                        DestinationRepository destinationRepository, ResourceStatusService resourceStatusService) {
        super(applicationContext, txManager);
        this.dispatchNotificationRepository = dispatchNotificationRepository;
        this.destinationRepository = destinationRepository;
        this.resourceStatusService = resourceStatusService;
    }

    @Override
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

    @Override
    public DispatchNotification dispatchAllResources(Assignment assignment) throws AssignmentNotReadyForDispatchException {
        return dispatchSelectedResources(assignment, findAssignedResources(assignment));
    }

    @Override
    public DispatchNotification dispatchAllReservedResources(Assignment assignment) throws AssignmentNotReadyForDispatchException {
        return dispatchSelectedResources(assignment, resourceStatusService.getStatusOfResourcesAssignedToAssignment(assignment.id())
                .stream()
                .filter(status -> status.state() == ResourceState.RESERVED)
                .map(ResourceStatus::resourceId)
                .toList()
        );
    }

    @Override
    public List<DispatchNotification> findDispatchNotificationsForAssignment(AssignmentId assignment) {
        logger.trace("Looking up dispatch notifications for assignment {}", assignment);
        return dispatchNotificationRepository.findByAssignment(assignment);
    }

    @Override
    public List<Receipt> findReceiptsForDispatchNotification(DispatchNotificationId dispatchNotification) {
        logger.debug("Looking up receipts for dispatch notification {}", dispatchNotification);
        return dispatchNotificationRepository.findReceipts(dispatchNotification);
    }

    private List<ResourceId> findAssignedResources(Assignment assignment) {
        return resourceStatusService.getResourcesAssignedToAssignment(assignment.id()).stream()
                .map(ResourceListItem::id).toList();
    }
}
