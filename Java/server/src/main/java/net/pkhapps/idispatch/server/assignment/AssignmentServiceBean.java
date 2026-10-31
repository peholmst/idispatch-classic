package net.pkhapps.idispatch.server.assignment;


import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeQueries;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityQueries;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.AbstractServiceBean;
import net.pkhapps.idispatch.server.shared.UpdateResult;
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
class AssignmentServiceBean extends AbstractServiceBean implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ResourcesOnAssignment resourcesOnAssignment;
    private final MunicipalityQueries municipalityQueries;
    private final AssignmentTypeQueries assignmentTypeQueries;

    AssignmentServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, AssignmentRepository assignmentRepository, ResourcesOnAssignment resourcesOnAssignment, MunicipalityQueries municipalityQueries, AssignmentTypeQueries assignmentTypeQueries) {
        super(applicationContext, txManager);
        this.assignmentRepository = assignmentRepository;
        this.resourcesOnAssignment = resourcesOnAssignment;
        this.municipalityQueries = municipalityQueries;
        this.assignmentTypeQueries = assignmentTypeQueries;
    }

    @Override
    public Optional<Assignment> findAssignment(Long id) {
        logger.debug("Looking up assignment with ID {}", id);
        if (id == null) {
            return Optional.empty();
        }
        return assignmentRepository.findById(id);
    }

    @Override
    public Long openAssignment() {
        logger.debug("Opening new assignment");
        final Assignment createdAssignment = getTxTemplate().execute(status -> assignmentRepository.saveAndFlush(new Assignment()));
        getApplicationContext().publishEvent(new AssignmentOpened(this, createdAssignment));
        logger.debug("Opened assignment with ID {}", createdAssignment.getId());
        return createdAssignment.getId();
    }

    @Override
    public UpdateResult<Assignment> updateAssignment(Assignment assignment) {
        logger.debug("Updating assignment {}", assignment);
        if (assignment.isAssignmentClosed()) {
            logger.debug("Assignment {} is closed, ignoring", assignment);
            return new UpdateResult.NoChange<>(assignment);
        }
        try {
            final Assignment savedAssignment = getTxTemplate().execute(status -> assignmentRepository.saveAndFlush(assignment));
            // Load the assignment again, so that the names of its type and municipality are those it now has
            final Assignment updatedAssignment = assignmentRepository.findById(savedAssignment.getId()).orElseThrow();
            getApplicationContext().publishEvent(new AssignmentUpdated(this, updatedAssignment));
            return new UpdateResult.Success<>(updatedAssignment);
        } catch (OptimisticLockingFailureException ex) {
            logger.debug("Assignment {} was updated by another user, cannot update", assignment);
            final Assignment updatedAssignment = assignmentRepository.findById(assignment.getId()).orElse(null);
            return new UpdateResult.Conflict<>(assignment, updatedAssignment);
        }
    }

    @Override
    public boolean closeAssignment(Assignment assignment) {
        return closeAssignment(assignment.getId());
    }

    private boolean closeAssignment(Long assignmentId) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null) {
            logger.debug("Could not close assignment - no such assignment ID: {}", assignmentId);
            return false;
        }
        logger.debug("Closing assignment {}", assignment);
        if (assignment.isAssignmentClosed()) {
            logger.debug("Assignment {} is already closed, ignoring", assignment);
            return false;
        }
        if (resourcesOnAssignment.anyAssignedTo(assignment.getAssignmentId())) {
            logger.debug("Resources are still assigned to assignment {}, ignoring", assignment);
            return false;
        }
        assignment.setClosed(Instant.now());
        final Assignment closedAssignment = getTxTemplate().execute(status -> assignmentRepository.saveAndFlush(assignment));
        resourcesOnAssignment.releaseAllFrom(closedAssignment.getAssignmentId());
        getApplicationContext().publishEvent(new AssignmentClosed(this, closedAssignment));
        return true;
    }

    @Override
    public List<Assignment> findOpenAssignments() {
        logger.debug("Looking up open assignments");
        return assignmentRepository.findByClosedIsNull();
    }

    @Override
    public List<MunicipalityListItem> findApplicableMunicipalities() {
        return municipalityQueries.findActive();
    }

    @Override
    public Optional<MunicipalityListItem> findMunicipality(MunicipalityId id) {
        return municipalityQueries.findById(id);
    }

    @Override
    public List<AssignmentTypeListItem> findApplicableAssignmentTypes() {
        return assignmentTypeQueries.findActive();
    }

    @Override
    public Optional<AssignmentTypeListItem> findAssignmentType(AssignmentTypeId id) {
        return assignmentTypeQueries.findById(id);
    }

    @Override
    public List<AssignmentUrgency> findApplicableUrgencies() {
        return List.of(AssignmentUrgency.values());
    }
}
