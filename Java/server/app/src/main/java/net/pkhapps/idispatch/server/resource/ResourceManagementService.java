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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the resources up to date. A call sign that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@PreAuthorize(Roles.HAS_ADMIN)
class ResourceManagementService {

    private static final Logger log = LoggerFactory.getLogger(ResourceManagementService.class);

    private final ResourceRepository repository;
    private final AuditLog auditLog;
    private final ResourceStatusRepository resourceStatusRepository;
    private final ResourceTypeRepository resourceTypeRepository;

    ResourceManagementService(ResourceRepository repository, ResourceStatusRepository resourceStatusRepository,
                              ResourceTypeRepository resourceTypeRepository, AuditLog auditLog) {
        this.auditLog = auditLog;
        this.repository = repository;
        this.resourceStatusRepository = resourceStatusRepository;
        this.resourceTypeRepository = resourceTypeRepository;
    }

    /**
     * Finds the resources matching the filter, ordered by call sign.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Resource> findById(ResourceId id) {
        return repository.findById(id);
    }

    /**
     * Adds a resource, which starts out of service.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resource create(ResourceDetails details) {
        log.debug("Creating resource {}", details.callSign());
        var resource = repository.insert(details);
        resourceStatusRepository.insert(
                new ResourceStatus(resource.id(), 0L, ResourceState.OUT_OF_SERVICE, null, Instant.now()));
        auditLog.record(audited(resource.id()), AuditEventKind.CREATED);
        return resource;
    }

    /**
     * Saves the changed details of the resource. If nothing has changed, saves nothing and returns the
     * resource as stored, with the same version.
     *
     * @return the resource as saved, with its new version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resource update(Resource resource) {
        log.debug("Updating resource {} version {}", resource.id().value(), resource.version());
        var stored = repository.findById(resource.id());
        if (stored.filter(resource::equals).isPresent()) {
            log.debug("Nothing to save in resource {}", resource.id().value());
            return stored.get();
        }
        var updated = repository.update(resource);
        auditLog.record(audited(resource.id()), AuditEventKind.EDITED);
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(ResourceId id) {
        log.debug("Softly deleting resource {}", id.value());
        if (repository.setActive(id, false)) {
            auditLog.record(audited(id), AuditEventKind.DELETED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(ResourceId id) {
        log.debug("Restoring resource {}", id.value());
        if (repository.setActive(id, true)) {
            auditLog.record(audited(id), AuditEventKind.RESTORED);
        }
    }

    /**
     * Finds the types that a resource can be given, which are the ones not deleted, ordered by code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<ResourceTypeListItem> findApplicableTypes() {
        return resourceTypeRepository.findListItems(false, null);
    }

    /**
     * Finds the type with the given ID, whether deleted or not.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<ResourceTypeListItem> findType(ResourceTypeId id) {
        return resourceTypeRepository.findListItem(id);
    }

    private static AuditedEntity audited(ResourceId id) {
        return new AuditedEntity(AuditedEntity.Type.RESOURCE, id.value());
    }
}
