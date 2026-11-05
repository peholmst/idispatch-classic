package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.audit.AuditEventKind;
import net.pkhapps.idispatch.server.shared.audit.AuditLog;
import net.pkhapps.idispatch.server.shared.audit.AuditedEntity;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the assignment types up to date. A code that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@PreAuthorize(Roles.HAS_ADMIN)
class AssignmentTypeManagementService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentTypeManagementService.class);

    private final AssignmentTypeRepository repository;
    private final AuditLog auditLog;

    AssignmentTypeManagementService(AssignmentTypeRepository repository, AuditLog auditLog) {
        this.auditLog = auditLog;
        this.repository = repository;
    }

    /**
     * Finds the assignment types matching the filter, ordered by code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<AssignmentTypeListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<AssignmentType> findById(AssignmentTypeId id) {
        return repository.findById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssignmentType create(AssignmentTypeDetails details) {
        log.debug("Creating assignment type {}", details.code());
        var created = repository.insert(details);
        auditLog.record(audited(created.id()), AuditEventKind.CREATED);
        return created;
    }

    /**
     * Saves the changed details of the type. If nothing has changed, saves nothing and returns the
     * type as stored, with the same version.
     *
     * @return the type as saved, with its new version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssignmentType update(AssignmentType type) {
        log.debug("Updating assignment type {} version {}", type.id().value(), type.version());
        var stored = repository.findById(type.id());
        if (stored.filter(type::equals).isPresent()) {
            log.debug("Nothing to save in type {}", type.id().value());
            return stored.get();
        }
        var updated = repository.update(type);
        auditLog.record(audited(type.id()), AuditEventKind.EDITED);
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(AssignmentTypeId id) {
        log.debug("Softly deleting assignment type {}", id.value());
        if (repository.setActive(id, false)) {
            auditLog.record(audited(id), AuditEventKind.DELETED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(AssignmentTypeId id) {
        log.debug("Restoring assignment type {}", id.value());
        if (repository.setActive(id, true)) {
            auditLog.record(audited(id), AuditEventKind.RESTORED);
        }
    }

    private static AuditedEntity audited(AssignmentTypeId id) {
        return new AuditedEntity(AuditedEntity.Type.ASSIGNMENT_TYPE, id.value());
    }
}
