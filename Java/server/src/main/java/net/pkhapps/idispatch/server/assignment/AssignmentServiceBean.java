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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Secured(Roles.ROLE_DISPATCHER)
class AssignmentServiceBean extends AbstractServiceBean implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ResourcesOnAssignment resourcesOnAssignment;
    private final MunicipalityQueries municipalityQueries;
    private final AssignmentTypeQueries assignmentTypeQueries;

    AssignmentServiceBean(ApplicationContext applicationContext, PlatformTransactionManager txManager,
                          AssignmentRepository assignmentRepository, ResourcesOnAssignment resourcesOnAssignment,
                          MunicipalityQueries municipalityQueries, AssignmentTypeQueries assignmentTypeQueries) {
        super(applicationContext, txManager);
        this.assignmentRepository = assignmentRepository;
        this.resourcesOnAssignment = resourcesOnAssignment;
        this.municipalityQueries = municipalityQueries;
        this.assignmentTypeQueries = assignmentTypeQueries;
    }

    @Override
    public Optional<Assignment> findAssignment(AssignmentId id) {
        return assignmentRepository.findById(id);
    }

    @Override
    public AssignmentId openAssignment() {
        logger.debug("Opening new assignment");
        var opened = getTxTemplate().execute(status -> assignmentRepository.insertNew());
        getApplicationContext().publishEvent(new AssignmentOpened(this, opened));
        logger.debug("Opened assignment with ID {}", opened.id().value());
        return opened.id();
    }

    @Override
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

    @Override
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

    @Override
    public List<AssignmentListItem> findOpenAssignments() {
        return assignmentRepository.findOpenListItems();
    }

    @Override
    public List<AssignmentListItem> findAssignments(Collection<AssignmentId> ids) {
        return ids.isEmpty() ? List.of() : assignmentRepository.findListItems(ids);
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
