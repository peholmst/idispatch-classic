package net.pkhapps.idispatch.server.assignment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Looks up assignments for the features that show them after the fact, such as the reports. Read-only and not
 * secured; the views and services that use it decide who may see what.
 */
@Service
@Transactional(readOnly = true)
public class AssignmentQueries {

    private final AssignmentRepository repository;

    AssignmentQueries(AssignmentRepository repository) {
        this.repository = repository;
    }

    public Optional<Assignment> findById(AssignmentId id) {
        return repository.findById(id);
    }
}
