package net.pkhapps.idispatch.server.resource;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractService;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Keeps the current state of each resource and the assignment it is on, archiving every state it leaves.
 */
@Service
@PreAuthorize(Roles.HAS_DISPATCHER)
public class ResourceStatusService extends AbstractService {

    private final ResourceStatusRepository resourceStatusRepository;
    private final ResourceQueries resourceQueries;

    ResourceStatusService(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                          ResourceStatusRepository resourceStatusRepository, ResourceQueries resourceQueries) {
        super(applicationContext, txManager);
        this.resourceStatusRepository = resourceStatusRepository;
        this.resourceQueries = resourceQueries;
    }

    /**
     * Finds the current status of the specified resource. If the resource is {@code null}, not active, or it does not
     * exist, an empty {@code Optional} is returned.
     *
     * @see Resource#active()
     */
    public Optional<ResourceStatus> findCurrentStatus(ResourceId resource) {
        logger.debug("Looking up current status of resource {}", resource);
        if (resource == null) {
            return Optional.empty();
        }
        return resourceStatusRepository.findOfActiveResource(resource);
    }

    /**
     * Attempts to set the state of the specified resource. If the requested state transition is not valid or the
     * resource is not active, nothing happens. Otherwise, the new state is saved and a {@link ResourceStatusChanged}
     * event is fired.
     *
     * @see ResourceStatus#allValidNextStates()
     * @see Resource#active()
     */
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

    /**
     * Attempts to assign the resource to the specified assignment, settings its state to
     * {@link ResourceState#RESERVED}. If the resource is not active or not available, nothing happens. Otherwise, the
     * new state is saved and a {@link ResourceStatusChanged} event is fired.
     *
     * @see ResourceStatus#isAvailable()
     * @see Resource#active()
     */
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

    /**
     * Attempts to clear the assignment from the specified resource, and stores the state and fires a
     * {@link ResourceStatusChanged} event if successful. This can only happen if the resource is active and is in any
     * of the following states:
     * <ul>
     * <li>{@link ResourceState#RESERVED}, in which case the state of the resource will change to whatever it was before the resource was reserved)</li>
     * <li>{@link ResourceState#AVAILABLE}</li>
     * <li>{@link ResourceState#AT_STATION}</li>
     * <li>{@link ResourceState#OUT_OF_SERVICE}</li>
     * </ul>
     * Otherwise, nothing happens.
     *
     * @see Resource#active()
     */
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

    /**
     * Same as {@link #clearResourceAssignment(ResourceId)}, but for all resources that refer to the specified
     * assignment.
     */
    public void clearAssignmentFromAllResources(AssignmentId assignment) {
        logger.debug("Attempting to clear the assignment from all resources associated with {}", assignment);
        resourceStatusRepository.findReferringTo(assignment)
                .forEach(status -> clearResourceAssignment(status.resourceId()));
    }

    /**
     * Finds all resources that are currently assigned to the specified assignment, in call sign order.
     *
     * @see ResourceStatus#isAssigned()
     */
    public List<ResourceListItem> getResourcesAssignedToAssignment(AssignmentId assignment) {
        logger.debug("Looking up resources assigned to {}", assignment);
        return toListItems(getStatusOfResourcesAssignedToAssignment(assignment));
    }

    /**
     * Finds the current status of all resources that are currently assigned to the specified assignment.
     *
     * @see ResourceStatus#isAssigned()
     */
    public List<ResourceStatus> getStatusOfResourcesAssignedToAssignment(AssignmentId assignment) {
        logger.debug("Looking up the status of the resources assigned to {}", assignment);
        return resourceStatusRepository.findAssignedTo(assignment);
    }

    /**
     * Finds all active and available resources, in call sign order.
     *
     * @see Resource#active()
     * @see ResourceStatus#isAvailable()
     */
    public List<ResourceListItem> getAvailableResources() {
        logger.debug("Looking up available resources");
        return toListItems(getStatusOfAvailableResources());
    }

    /**
     * Finds the current status of all active and available resources.
     *
     * @see Resource#active()
     * @see ResourceStatus#isAvailable()
     */
    public List<ResourceStatus> getStatusOfAvailableResources() {
        logger.debug("Looking up the status of the available resources");
        return resourceStatusRepository.findOfActiveResources(true);
    }

    /**
     * Finds all active resources, in call sign order.
     *
     * @see Resource#active()
     */
    public List<ResourceListItem> getAllResources() {
        logger.debug("Looking up all resources");
        return toListItems(getStatusOfAllResources());
    }

    /**
     * Finds the current status of all active resources.
     *
     * @see Resource#active()
     */
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
