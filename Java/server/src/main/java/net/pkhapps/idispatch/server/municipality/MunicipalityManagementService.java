package net.pkhapps.idispatch.server.municipality;

import net.pkhapps.idispatch.server.security.Roles;
import net.pkhapps.idispatch.server.shared.management.DefaultFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;
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
@Secured(Roles.ROLE_ADMIN)
public class MunicipalityManagementService {

    private static final Logger log = LoggerFactory.getLogger(MunicipalityManagementService.class);

    private final MunicipalityRepository repository;

    MunicipalityManagementService(MunicipalityRepository repository) {
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
        return repository.insert(details);
    }

    /**
     * Saves the changed details of the municipality.
     *
     * @return the municipality as saved, with its new version
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Municipality update(Municipality municipality) {
        log.debug("Updating municipality {} version {}", municipality.id().value(), municipality.version());
        return repository.update(municipality);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(MunicipalityId id) {
        log.debug("Softly deleting municipality {}", id.value());
        repository.setActive(id, false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(MunicipalityId id) {
        log.debug("Restoring municipality {}", id.value());
        repository.setActive(id, true);
    }
}
