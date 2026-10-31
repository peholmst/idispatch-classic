package net.pkhapps.idispatch.server.assignmenttype;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
class AssignmentTypeQueriesBean implements AssignmentTypeQueries {

    private final AssignmentTypeRepository repository;

    AssignmentTypeQueriesBean(AssignmentTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AssignmentTypeListItem> findActive() {
        return repository.findListItems(false, null);
    }

    @Override
    public Optional<AssignmentTypeListItem> findById(AssignmentTypeId id) {
        return repository.findListItem(id);
    }

    @Override
    public List<AssignmentTypeListItem> findByIds(Collection<AssignmentTypeId> ids) {
        return ids.isEmpty() ? List.of() : repository.findListItems(ids);
    }
}
