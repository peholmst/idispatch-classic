package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Secured(Roles.ROLE_DISPATCHER)
class ResourceStatusServiceBean extends AbstractServiceBean implements ResourceStatusService {

    private final ResourceStatusRepository resourceStatusRepository;
    private final ArchivedResourceStatusRepository archivedResourceStatusRepository;

    ResourceStatusServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, ResourceStatusRepository resourceStatusRepository, ArchivedResourceStatusRepository archivedResourceStatusRepository) {
        super(applicationContext, txManager);
        this.resourceStatusRepository = resourceStatusRepository;
        this.archivedResourceStatusRepository = archivedResourceStatusRepository;
    }

    @Override
    public Optional<ResourceStatus> findCurrentStatus(Resource resource) {
        logger.debug("Looking up current status of resource {}", resource);
        if (resource == null || !resource.isActive()) {
            return Optional.empty();
        }
        final ResourceStatus resourceStatus = resourceStatusRepository.findByResource(resource);
        return Optional.ofNullable(resourceStatus);
    }

    @Override
    public void setResourceState(Resource resource, ResourceState resourceState) {
        logger.debug("Attempting to set the state of resource {} to {}", resource, resourceState);
        attemptStatusChangeOperation(resource, status -> {
            if (status.getAllValidNextStates().contains(resourceState)) {
                status.setState(resourceState);
                return true;
            }
            logger.debug("Cannot change the state of resource {} to {}", resource, resourceState);
            return false;
        });
    }

    @Override
    public void setResourceAssignment(Resource resource, AssignmentId assignment) {
        logger.debug("Attempting to assign resource {} to assignment {}", resource, assignment);
        attemptStatusChangeOperation(resource, status -> {
            if (status.isAvailable()) {
                status.setState(ResourceState.RESERVED);
                status.setAssignmentId(assignment);
                return true;
            }
            logger.debug("Cannot assign resource {} to assignment {}", resource, assignment);
            return false;
        });
    }

    @Override
    public void clearResourceAssignment(Resource resource) {
        logger.debug("Attempting to clear the assignment from resource {}", resource);
        attemptStatusChangeOperation(resource, status -> {
            switch (status.getState()) {
                case RESERVED:
                    final Optional<ArchivedResourceStatus> previousResourceStatus = findPreviousResourceStatus(resource);
                    if (previousResourceStatus.isPresent()) {
                        final ResourceState oldState = previousResourceStatus.get().getState();
                        logger.debug("Reverting the state of resource {} to {}", resource, oldState);
                        status.setState(oldState);
                    } else {
                        return false;
                    }
                case AVAILABLE:
                case AT_STATION:
                case OUT_OF_SERVICE:
                    status.setAssignmentId(null);
                    return true;
                default:
                    logger.debug("Resource {} was in state {}, cannot clear assignment", resource, status.getState());
                    return false;
            }
        });
    }

    @Override
    public void clearAssignmentFromAllResources(AssignmentId assignment) {
        logger.debug("Attempting to clear the assignment from all resources associated with {}", assignment);
        resourceStatusRepository.findByAssignmentId(assignment.value()).forEach(status -> clearResourceAssignment(status.getResource()));
    }

    @Override
    public List<Resource> getResourcesAssignedToAssignment(AssignmentId assignment) {
        logger.debug("Looking up resources assigned to {}", assignment);
        return getStatusOfResourcesAssignedToAssignment(assignment)
                .stream()
                .map(ResourceStatus::getResource)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceStatus> getStatusOfResourcesAssignedToAssignment(AssignmentId assignment) {
        logger.debug("Looking up the status of the resources assigned to {}", assignment);
        return resourceStatusRepository.findByAssignmentIdAndAssignedTrue(assignment.value());
    }

    @Override
    public List<Resource> getAvailableResources() {
        logger.debug("Looking up available resources");
        return getStatusOfAvailableResources()
                .stream()
                .map(ResourceStatus::getResource)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceStatus> getStatusOfAvailableResources() {
        logger.debug("Looking up the status of the available resources");
        return resourceStatusRepository.findByActiveTrueAndAvailableTrue();
    }

    @Override
    public List<Resource> getAllResources() {
        logger.debug("Looking up all resources");
        return getStatusOfAllResources()
                .stream()
                .map(ResourceStatus::getResource)
                .collect(Collectors.toList());
    }

    @Override
    public List<ResourceStatus> getStatusOfAllResources() {
        logger.debug("Looking up the status of all resources");
        return resourceStatusRepository.findByActiveTrue();
    }

    private void attemptStatusChangeOperation(Resource resource, StatusChangeOperation operation) {
        findCurrentStatus(resource)
                .flatMap(status -> executeStatusChangeOperation(status, operation))
                .ifPresent(updatedStatus -> getApplicationContext().publishEvent(new ResourceStatusChanged(ResourceStatusServiceBean.this, updatedStatus)));
    }

    private Optional<ResourceStatus> executeStatusChangeOperation(ResourceStatus currentStatus, StatusChangeOperation operation) {
        final ArchivedResourceStatus archivedResourceStatus = currentStatus.toArchived();
        return getTxTemplate().execute(tx -> {
            if (operation.doOnActiveResourceStatus(currentStatus)) {
                try {
                    ResourceStatus updatedStatus = resourceStatusRepository.saveAndFlush(currentStatus);
                    archivedResourceStatusRepository.saveAndFlush(archivedResourceStatus);
                    return Optional.of(updatedStatus);
                } catch (OptimisticLockingFailureException ex) {
                    tx.setRollbackOnly();
                }
            }
            return Optional.empty();
        });
    }

    private Optional<ArchivedResourceStatus> findPreviousResourceStatus(Resource resource) {
        List<ArchivedResourceStatus> result = archivedResourceStatusRepository.findByResource(resource, PageRequest.of(0, 1, Sort.Direction.DESC, ArchivedResourceStatus.PROP_TIMESTAMP));
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(result.iterator().next());
        }
    }

    @FunctionalInterface
    private interface StatusChangeOperation {
        boolean doOnActiveResourceStatus(ResourceStatus resourceStatus);
    }
}
