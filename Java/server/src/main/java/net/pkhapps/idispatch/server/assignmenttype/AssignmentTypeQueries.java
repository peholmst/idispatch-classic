package net.pkhapps.idispatch.server.assignmenttype;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Looks up assignment types for the features that show them, such as the dispatcher's assignment form, the reports and
 * the dispatch notifications. Read-only and not secured, as it also serves dispatching in the background; the views
 * and services that use it decide who may see what.
 */
@Service
@Transactional(readOnly = true)
public class AssignmentTypeQueries {

    private final AssignmentTypeRepository repository;

    AssignmentTypeQueries(AssignmentTypeRepository repository) {
        this.repository = repository;
    }

    /**
     * Finds the types that an assignment can be given, which are the ones not deleted, ordered by code.
     */
    public List<AssignmentTypeListItem> findActive() {
        return repository.findListItems(false, null);
    }

    /**
     * Finds the type with the given ID, whether deleted or not.
     */
    public Optional<AssignmentTypeListItem> findById(AssignmentTypeId id) {
        return repository.findListItem(id);
    }

    /**
     * Finds the types with the given IDs, whether deleted or not, ordered by code.
     */
    public List<AssignmentTypeListItem> findByIds(Collection<AssignmentTypeId> ids) {
        return ids.isEmpty() ? List.of() : repository.findListItems(ids);
    }
}
