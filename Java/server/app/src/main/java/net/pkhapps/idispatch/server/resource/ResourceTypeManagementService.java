package net.pkhapps.idispatch.server.resource;

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
 * Lets administrators keep the resource types up to date. A code that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@PreAuthorize(Roles.HAS_ADMIN)
class ResourceTypeManagementService {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeManagementService.class);

    private final ResourceTypeRepository repository;
    private final AuditLog auditLog;

    ResourceTypeManagementService(ResourceTypeRepository repository, AuditLog auditLog) {
        this.auditLog = auditLog;
        this.repository = repository;
    }

    /**
     * Finds the resource types matching the filter, ordered by code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceTypeListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ResourceType> findById(ResourceTypeId id) {
        return repository.findById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ResourceType create(ResourceTypeDetails details) {
        log.debug("Creating resource type {}", details.code());
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
    public ResourceType update(ResourceType type) {
        log.debug("Updating resource type {} version {}", type.id().value(), type.version());
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
    public void delete(ResourceTypeId id) {
        log.debug("Softly deleting resource type {}", id.value());
        if (repository.setActive(id, false)) {
            auditLog.record(audited(id), AuditEventKind.DELETED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(ResourceTypeId id) {
        log.debug("Restoring resource type {}", id.value());
        if (repository.setActive(id, true)) {
            auditLog.record(audited(id), AuditEventKind.RESTORED);
        }
    }

    private static AuditedEntity audited(ResourceTypeId id) {
        return new AuditedEntity(AuditedEntity.Type.RESOURCE_TYPE, id.value());
    }
}
