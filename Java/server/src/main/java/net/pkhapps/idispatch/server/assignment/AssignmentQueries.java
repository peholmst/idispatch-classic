package net.pkhapps.idispatch.server.assignment;

import java.util.Optional;

/**
 * Looks up assignments for the features that show them after the fact, such as the reports. Read-only and not
 * secured; the views and services that use it decide who may see what.
 */
public interface AssignmentQueries {

    Optional<Assignment> findById(AssignmentId id);
}
