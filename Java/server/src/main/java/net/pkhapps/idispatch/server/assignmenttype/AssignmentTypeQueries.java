package net.pkhapps.idispatch.server.assignmenttype;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Looks up assignment types for the features that show them, such as the dispatcher's assignment form, the reports and
 * the dispatch notifications. Read-only and not secured, as it also serves dispatching in the background; the views
 * and services that use it decide who may see what.
 */
public interface AssignmentTypeQueries {

    /**
     * Finds the types that an assignment can be given, which are the ones not deleted, ordered by code.
     */
    List<AssignmentTypeListItem> findActive();

    /**
     * Finds the type with the given ID, whether deleted or not.
     */
    Optional<AssignmentTypeListItem> findById(AssignmentTypeId id);

    /**
     * Finds the types with the given IDs, whether deleted or not, ordered by code.
     */
    List<AssignmentTypeListItem> findByIds(Collection<AssignmentTypeId> ids);
}
