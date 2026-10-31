package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractServiceBean;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Secured(Roles.ROLE_DISPATCHER)
class ResourceStatusServiceBean extends AbstractServiceBean implements ResourceStatusService {

    private final ResourceStatusRepository resourceStatusRepository;
    private final ResourceQueries resourceQueries;

    ResourceStatusServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                              ResourceStatusRepository resourceStatusRepository, ResourceQueries resourceQueries) {
        super(applicationContext, txManager);
        this.resourceStatusRepository = resourceStatusRepository;
        this.resourceQueries = resourceQueries;
    }

    @Override
    public Optional<ResourceStatus> findCurrentStatus(ResourceId resource) {
        logger.debug("Looking up current status of resource {}", resource);
        if (resource == null) {
            return Optional.empty();
        }
        return resourceStatusRepository.findOfActiveResource(resource);
    }

    @Override
    public void setResourceState(ResourceId resource, ResourceState resourceState) {
        logger.debug("Attempting to set the state of resource {} to {}", resource, resourceState);
        attemptStatusChange(resource, (status, now) -> {
            if (status.allValidNextStates().contains(resourceState)) {
                return Optional.of(status.withState(resourceState, now));
            }
            logger.debug("Cannot change the state of resource {} to {}", resource, resourceState);
            return Optional.empty();
        });
    }

    @Override
    public void setResourceAssignment(ResourceId resource, AssignmentId assignment) {
        logger.debug("Attempting to assign resource {} to assignment {}", resource, assignment);
        attemptStatusChange(resource, (status, now) -> {
            if (status.isAvailable()) {
                return Optional.of(status.withState(ResourceState.RESERVED, now).withAssignment(assignment, now));
            }
            logger.debug("Cannot assign resource {} to assignment {}", resource, assignment);
            return Optional.empty();
        });
    }

    @Override
    public void clearResourceAssignment(ResourceId resource) {
        logger.debug("Attempting to clear the assignment from resource {}", resource);
        attemptStatusChange(resource, (status, now) -> switch (status.state()) {
            // A reserved resource goes back to the state it had before it was reserved
            case RESERVED -> resourceStatusRepository.findLatestArchived(resource)
                    .map(previous -> {
                        logger.debug("Reverting the state of resource {} to {}", resource, previous.state());
                        return status.withState(previous.state(), now).withAssignment(null, now);
                    });
            case AVAILABLE, AT_STATION, OUT_OF_SERVICE -> Optional.of(status.withAssignment(null, now));
            default -> {
                logger.debug("Resource {} was in state {}, cannot clear assignment", resource, status.state());
                yield Optional.empty();
            }
        });
    }

    @Override
    public void clearAssignmentFromAllResources(AssignmentId assignment) {
        logger.debug("Attempting to clear the assignment from all resources associated with {}", assignment);
        resourceStatusRepository.findReferringTo(assignment)
                .forEach(status -> clearResourceAssignment(status.resourceId()));
    }

    @Override
    public List<ResourceListItem> getResourcesAssignedToAssignment(AssignmentId assignment) {
        logger.debug("Looking up resources assigned to {}", assignment);
        return toListItems(getStatusOfResourcesAssignedToAssignment(assignment));
    }

    @Override
    public List<ResourceStatus> getStatusOfResourcesAssignedToAssignment(AssignmentId assignment) {
        logger.debug("Looking up the status of the resources assigned to {}", assignment);
        return resourceStatusRepository.findAssignedTo(assignment);
    }

    @Override
    public List<ResourceListItem> getAvailableResources() {
        logger.debug("Looking up available resources");
        return toListItems(getStatusOfAvailableResources());
    }

    @Override
    public List<ResourceStatus> getStatusOfAvailableResources() {
        logger.debug("Looking up the status of the available resources");
        return resourceStatusRepository.findOfActiveResources(true);
    }

    @Override
    public List<ResourceListItem> getAllResources() {
        logger.debug("Looking up all resources");
        return toListItems(getStatusOfAllResources());
    }

    @Override
    public List<ResourceStatus> getStatusOfAllResources() {
        logger.debug("Looking up the status of all resources");
        return resourceStatusRepository.findOfActiveResources(false);
    }

    private List<ResourceListItem> toListItems(List<ResourceStatus> statuses) {
        return resourceQueries.findByIds(statuses.stream().map(ResourceStatus::resourceId).toList());
    }

    /**
     * Changes the status of the resource as the change says, unless the resource is deleted or the change refuses.
     * The previous status is archived. If another change got there first, nothing happens.
     */
    private void attemptStatusChange(ResourceId resource, StatusChange change) {
        Optional<ResourceStatus> updated = getTxTemplate().execute(tx -> {
            try {
                return findCurrentStatus(resource).flatMap(current -> change.apply(current, Instant.now())
                        .map(next -> {
                            var saved = resourceStatusRepository.update(next);
                            resourceStatusRepository.archive(current.toArchived());
                            return saved;
                        }));
            } catch (OptimisticLockingFailureException ex) {
                logger.debug("The status of resource {} was changed at the same time", resource);
                tx.setRollbackOnly();
                return Optional.empty();
            }
        });
        if (updated != null) {
            updated.ifPresent(status -> getApplicationContext().publishEvent(new ResourceStatusChanged(this, status)));
        }
    }

    @FunctionalInterface
    private interface StatusChange {
        /**
         * Returns the next status, or nothing if the change cannot be made from the current one.
         */
        Optional<ResourceStatus> apply(ResourceStatus current, Instant now);
    }
}
