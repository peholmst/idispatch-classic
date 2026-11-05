package net.pkhapps.idispatch.server.municipality;

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
 * Lets administrators keep the municipalities up to date. A name that another municipality already has in the same
 * language is refused with a {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an
 * outdated version with an {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
@Service
@PreAuthorize(Roles.HAS_ADMIN)
public class MunicipalityManagementService {

    private static final Logger log = LoggerFactory.getLogger(MunicipalityManagementService.class);

    private final MunicipalityRepository repository;
    private final AuditLog auditLog;

    MunicipalityManagementService(MunicipalityRepository repository, AuditLog auditLog) {
        this.auditLog = auditLog;
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<MunicipalityListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Municipality> findById(MunicipalityId id) {
        return repository.findById(id);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Municipality create(MunicipalityDetails details) {
        log.debug("Creating municipality {}", details.name());
        var created = repository.insert(details);
        auditLog.record(audited(created.id()), AuditEventKind.CREATED);
        return created;
    }

    /**
     * Saves the changed details of the municipality. If nothing has changed, saves nothing and returns the
     * municipality as stored, with the same version.
     *
     * @return the municipality as saved, with its new version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Municipality update(Municipality municipality) {
        log.debug("Updating municipality {} version {}", municipality.id().value(), municipality.version());
        var stored = repository.findById(municipality.id());
        if (stored.filter(municipality::equals).isPresent()) {
            log.debug("Nothing to save in municipality {}", municipality.id().value());
            return stored.get();
        }
        var updated = repository.update(municipality);
        auditLog.record(audited(municipality.id()), AuditEventKind.EDITED);
        return updated;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(MunicipalityId id) {
        log.debug("Softly deleting municipality {}", id.value());
        if (repository.setActive(id, false)) {
            auditLog.record(audited(id), AuditEventKind.DELETED);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(MunicipalityId id) {
        log.debug("Restoring municipality {}", id.value());
        if (repository.setActive(id, true)) {
            auditLog.record(audited(id), AuditEventKind.RESTORED);
        }
    }

    private static AuditedEntity audited(MunicipalityId id) {
        return new AuditedEntity(AuditedEntity.Type.MUNICIPALITY, id.value());
    }
}
