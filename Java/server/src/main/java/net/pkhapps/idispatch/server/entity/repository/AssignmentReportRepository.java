package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository of {@link AssignmentReport}s.
 */
public interface AssignmentReportRepository extends JpaRepository<AssignmentReport, Long> {

    Optional<AssignmentReport> findByAssignment(Assignment assignment);
}
