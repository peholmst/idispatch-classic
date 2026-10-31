package net.pkhapps.idispatch.server.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository of {@link AssignmentReport}s.
 */
interface AssignmentReportRepository extends JpaRepository<AssignmentReport, Long> {

    Optional<AssignmentReport> findByAssignmentId(Long assignmentId);

}
