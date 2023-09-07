package net.pkhapps.idispatch.server.boundary;

import jakarta.validation.Validator;
import net.pkhapps.idispatch.server.entity.*;
import net.pkhapps.idispatch.server.entity.repository.DestinationRepository;
import net.pkhapps.idispatch.server.entity.repository.DispatchNotificationRepository;
import net.pkhapps.idispatch.server.entity.repository.ReceiptRepository;
import net.pkhapps.idispatch.server.events.DispatchNotificationSent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newHashSet;

@Service
class DispatchServiceBean extends AbstractServiceBean implements DispatchService {

    private final DispatchNotificationRepository dispatchNotificationRepository;
    private final DestinationRepository destinationRepository;
    private final ReceiptRepository receiptRepository;
    private final ResourceStatusService resourceStatusService;
    private final Validator validator;

    DispatchServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, DispatchNotificationRepository dispatchNotificationRepository, DestinationRepository destinationRepository, ReceiptRepository receiptRepository, ResourceStatusService resourceStatusService, Validator validator) {
        super(applicationContext, txManager);
        this.dispatchNotificationRepository = dispatchNotificationRepository;
        this.destinationRepository = destinationRepository;
        this.receiptRepository = receiptRepository;
        this.resourceStatusService = resourceStatusService;
        this.validator = validator;
    }

    @Override
    public DispatchNotification dispatchSelectedResources(Assignment assignment, Collection<Resource> resources) throws ValidationFailedException {
        ValidationFailedException.throwIfNotEmpty(validator.validate(assignment, DispatchValidationGroup.class));
        final Collection<Resource> assignedResources = newHashSet(findAssignedResources(assignment));
        final Collection<Resource> resourcesToDispatch = resources.stream()
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
    public DispatchNotification dispatchAllResources(Assignment assignment) throws ValidationFailedException {
        return dispatchSelectedResources(assignment, resourceStatusService.getResourcesAssignedToAssignment(assignment));
    }

    @Override
    public DispatchNotification dispatchAllReservedResources(Assignment assignment) throws ValidationFailedException {
        return dispatchSelectedResources(assignment, resourceStatusService.getStatusOfResourcesAssignedToAssignment(assignment)
                .stream()
                .filter(status -> status.getState() == ResourceState.RESERVED)
                .map(ResourceStatus::getResource)
                .collect(Collectors.toList())
        );
    }

    @Override
    public List<DispatchNotification> findDispatchNotificationsForAssignment(Assignment assignment) {
        logger.trace("Looking up dispatch notifications for assignment {}", assignment);
        return dispatchNotificationRepository.findByAssignment(assignment);
    }

    @Override
    public List<Receipt> findReceiptsForDispatchNotification(DispatchNotification dispatchNotification) {
        logger.debug("Looking up receipts for dispatch notification {}", dispatchNotification);
        return receiptRepository.findByNotification(dispatchNotification);
    }

    private Collection<Destination> findDestinations(Collection<Resource> resources) {
        return resources.stream()
                .flatMap(resource -> destinationRepository.findDestinationsForResource(resource).stream())
                .collect(Collectors.toSet());
    }

    private Collection<Resource> findAssignedResources(Assignment assignment) {
        return resourceStatusService.getResourcesAssignedToAssignment(assignment);
    }
}
