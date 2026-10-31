package net.pkhapps.idispatch.server.assignment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
class AssignmentQueriesBean implements AssignmentQueries {

    private final AssignmentRepository repository;

    AssignmentQueriesBean(AssignmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Assignment> findById(AssignmentId id) {
        return repository.findById(id);
    }
}
