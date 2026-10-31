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

@Service
@Secured(Roles.ROLE_ADMIN)
class MunicipalityManagementServiceBean implements MunicipalityManagementService {

    private static final Logger log = LoggerFactory.getLogger(MunicipalityManagementServiceBean.class);

    private final MunicipalityRepository repository;

    MunicipalityManagementServiceBean(MunicipalityRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<MunicipalityListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Municipality> findById(MunicipalityId id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Municipality create(MunicipalityDetails details) {
        log.debug("Creating municipality {}", details.name());
        return repository.insert(details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Municipality update(Municipality municipality) {
        log.debug("Updating municipality {} version {}", municipality.id().value(), municipality.version());
        return repository.update(municipality);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(MunicipalityId id) {
        log.debug("Softly deleting municipality {}", id.value());
        repository.setActive(id, false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(MunicipalityId id) {
        log.debug("Restoring municipality {}", id.value());
        repository.setActive(id, true);
    }
}
