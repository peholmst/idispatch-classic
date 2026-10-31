package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeQueries;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityQueries;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractService;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Lets dispatchers open, fill in and close assignments.
 */
@Service
@Secured(Roles.ROLE_DISPATCHER)
public class AssignmentService extends AbstractService {

    private final AssignmentRepository assignmentRepository;
    private final ResourcesOnAssignment resourcesOnAssignment;
    private final MunicipalityQueries municipalityQueries;
    private final AssignmentTypeQueries assignmentTypeQueries;

    AssignmentService(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                      AssignmentRepository assignmentRepository, ResourcesOnAssignment resourcesOnAssignment,
                      MunicipalityQueries municipalityQueries, AssignmentTypeQueries assignmentTypeQueries) {
        super(applicationContext, txManager);
        this.assignmentRepository = assignmentRepository;
        this.resourcesOnAssignment = resourcesOnAssignment;
        this.municipalityQueries = municipalityQueries;
        this.assignmentTypeQueries = assignmentTypeQueries;
    }

    public Optional<Assignment> findAssignment(AssignmentId id) {
        return assignmentRepository.findById(id);
    }

    /**
     * Opens a new, empty assignment, publishes an {@link AssignmentOpened} event and returns the assignment's ID.
     */
    public AssignmentId openAssignment() {
        logger.debug("Opening new assignment");
        var opened = getTxTemplate().execute(status -> assignmentRepository.insertNew());
        getApplicationContext().publishEvent(new AssignmentOpened(this, opened));
        logger.debug("Opened assignment with ID {}", opened.id().value());
        return opened.id();
    }

    /**
     * Saves what a dispatcher has filled in, publishes an {@link AssignmentUpdated} event and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the assignment as saved, with its new version.</li>
     * <li>{@link UpdateResult.Conflict} with the assignment as it now is, if it had been changed by another user.</li>
     * <li>{@link UpdateResult.NoChange} if the assignment is closed.</li>
     * </ul>
     */
    public UpdateResult<Assignment> updateAssignment(Assignment assignment) {
        logger.debug("Updating assignment {}", assignment);
        if (assignment.isClosed()) {
            logger.debug("Assignment {} is closed, ignoring", assignment.id().value());
            return new UpdateResult.NoChange<>(assignment);
        }
        try {
            var updated = getTxTemplate().execute(status -> assignmentRepository.update(assignment));
            getApplicationContext().publishEvent(new AssignmentUpdated(this, updated));
            return new UpdateResult.Success<>(updated);
        } catch (OptimisticLockingFailureException ex) {
            logger.debug("Assignment {} was changed by another user, cannot update", assignment.id().value());
            return new UpdateResult.Conflict<>(assignment,
                    assignmentRepository.findById(assignment.id()).orElse(null));
        }
    }

    /**
     * Closes the assignment and publishes an {@link AssignmentClosed} event. If the assignment is already closed, or
     * resources are still assigned to it, nothing happens and false is returned.
     */
    public boolean closeAssignment(AssignmentId id) {
        var assignment = assignmentRepository.findById(id).orElse(null);
        if (assignment == null || assignment.isClosed()) {
            logger.debug("No open assignment with ID {} to close", id.value());
            return false;
        }
        if (resourcesOnAssignment.anyAssignedTo(id)) {
            logger.debug("Resources are still assigned to assignment {}, ignoring", id.value());
            return false;
        }
        var closed = getTxTemplate().execute(status -> assignmentRepository.close(id)).orElse(null);
        if (closed == null) {
            return false;
        }
        resourcesOnAssignment.releaseAllFrom(id);
        getApplicationContext().publishEvent(new AssignmentClosed(this, closed));
        return true;
    }

    /**
     * Finds the open assignments, oldest first.
     */
    public List<AssignmentListItem> findOpenAssignments() {
        return assignmentRepository.findOpenListItems();
    }

    /**
     * Finds the assignments with the given IDs, open or closed, oldest first.
     */
    public List<AssignmentListItem> findAssignments(Collection<AssignmentId> ids) {
        return ids.isEmpty() ? List.of() : assignmentRepository.findListItems(ids);
    }

    /**
     * Finds the municipalities that an assignment can be in, which are the ones not deleted, in no particular order.
     */
    public List<MunicipalityListItem> findApplicableMunicipalities() {
        return municipalityQueries.findActive();
    }

    /**
     * Finds the municipality with the given ID, whether deleted or not, so that an assignment keeps showing the
     * municipality it is in.
     */
    public Optional<MunicipalityListItem> findMunicipality(MunicipalityId id) {
        return municipalityQueries.findById(id);
    }

    /**
     * Finds the types that an assignment can be given, which are the ones not deleted, ordered by code.
     */
    public List<AssignmentTypeListItem> findApplicableAssignmentTypes() {
        return assignmentTypeQueries.findActive();
    }

    /**
     * Finds the type with the given ID, whether deleted or not, so that an assignment keeps showing the type it has.
     */
    public Optional<AssignmentTypeListItem> findAssignmentType(AssignmentTypeId id) {
        return assignmentTypeQueries.findById(id);
    }

    public List<AssignmentUrgency> findApplicableUrgencies() {
        return List.of(AssignmentUrgency.values());
    }
}
