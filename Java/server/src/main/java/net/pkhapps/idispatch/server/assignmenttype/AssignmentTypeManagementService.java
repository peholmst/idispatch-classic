package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.shared.management.DefaultFilter;

import java.util.List;
import java.util.Optional;

/**
 * Lets administrators keep the assignment types up to date. A code that is already in use is refused with a
 * {@link org.springframework.dao.DataIntegrityViolationException}, and a change based on an outdated version with an
 * {@link org.springframework.dao.OptimisticLockingFailureException}.
 */
public interface AssignmentTypeManagementService {

    /**
     * Finds the assignment types matching the filter, ordered by code.
     */
    List<AssignmentTypeListItem> findAll(DefaultFilter filter);

    Optional<AssignmentType> findById(AssignmentTypeId id);

    AssignmentType create(AssignmentTypeDetails details);

    /**
     * Saves the changed details of the type.
     *
     * @return the type as saved, with its new version
     */
    AssignmentType update(AssignmentType type);

    void delete(AssignmentTypeId id);

    void restore(AssignmentTypeId id);
}
