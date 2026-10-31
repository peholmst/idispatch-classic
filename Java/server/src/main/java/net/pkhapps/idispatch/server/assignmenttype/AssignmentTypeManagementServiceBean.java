package net.pkhapps.idispatch.server.assignmenttype;

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
class AssignmentTypeManagementServiceBean implements AssignmentTypeManagementService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentTypeManagementServiceBean.class);

    private final AssignmentTypeRepository repository;

    AssignmentTypeManagementServiceBean(AssignmentTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<AssignmentTypeListItem> findAll(DefaultFilter filter) {
        var searchTerm = filter.searchTerm() == null ? "" : filter.searchTerm().trim();
        return repository.findListItems(filter.includesDeleted(), searchTerm);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<AssignmentType> findById(AssignmentTypeId id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssignmentType create(AssignmentTypeDetails details) {
        log.debug("Creating assignment type {}", details.code());
        return repository.insert(details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssignmentType update(AssignmentType type) {
        log.debug("Updating assignment type {} version {}", type.id().value(), type.version());
        return repository.update(type);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void delete(AssignmentTypeId id) {
        log.debug("Softly deleting assignment type {}", id.value());
        repository.setActive(id, false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restore(AssignmentTypeId id) {
        log.debug("Restoring assignment type {}", id.value());
        repository.setActive(id, true);
    }
}
