package net.pkhapps.idispatch.server.boundary;


import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.AssignmentUrgency;
import net.pkhapps.idispatch.server.entity.Municipality;
import net.pkhapps.idispatch.server.entity.repository.AssignmentRepository;
import net.pkhapps.idispatch.server.entity.repository.AssignmentTypeRepository;
import net.pkhapps.idispatch.server.entity.repository.MunicipalityRepository;
import net.pkhapps.idispatch.server.events.AssignmentClosed;
import net.pkhapps.idispatch.server.events.AssignmentOpened;
import net.pkhapps.idispatch.server.events.AssignmentUpdated;
import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.util.UpdateResult;
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
    private final ResourceStatusService resourceStatusService;
    private final MunicipalityRepository municipalityRepository;
    private final AssignmentTypeRepository assignmentTypeRepository;

    AssignmentServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager, AssignmentRepository assignmentRepository, ResourceStatusService resourceStatusService, MunicipalityRepository municipalityRepository, AssignmentTypeRepository assignmentTypeRepository) {
        super(applicationContext, txManager);
        this.assignmentRepository = assignmentRepository;
        this.resourceStatusService = resourceStatusService;
        this.municipalityRepository = municipalityRepository;
        this.assignmentTypeRepository = assignmentTypeRepository;
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
            final Assignment updatedAssignment = getTxTemplate().execute(status -> assignmentRepository.saveAndFlush(assignment));
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
        if (!resourceStatusService.getStatusOfResourcesAssignedToAssignment(assignment).isEmpty()) {
            logger.debug("Resources are still assigned to assignment {}, ignoring", assignment);
            return false;
        }
        assignment.setClosed(Instant.now());
        final Assignment closedAssignment = getTxTemplate().execute(status -> assignmentRepository.saveAndFlush(assignment));
        resourceStatusService.clearAssignmentFromAllResources(closedAssignment);
        getApplicationContext().publishEvent(new AssignmentClosed(this, closedAssignment));
        return true;
    }

    @Override
    public List<Assignment> findOpenAssignments() {
        logger.debug("Looking up open assignments");
        return assignmentRepository.findByClosedIsNull();
    }

    @Override
    @Secured({Roles.ROLE_DISPATCHER, Roles.ROLE_REPORT_READER})
    public List<Assignment> findClosedAssignments() {
        logger.debug("Looking up closed assignments");
        return assignmentRepository.findByClosedIsNotNull();
    }

    @Override
    public List<Municipality> findApplicableMunicipalities() {
        return municipalityRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    public List<AssignmentType> findApplicableAssignmentTypes() {
        return assignmentTypeRepository.findByActiveTrueOrderByCodeAsc();
    }

    @Override
    public List<AssignmentUrgency> findApplicableUrgencies() {
        return List.of(AssignmentUrgency.values());
    }
}
