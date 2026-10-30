package net.pkhapps.idispatch.server.entity.repository;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.AssignmentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.Set;

/**
 * Repository of {@link AssignmentReport}s.
 */
public interface AssignmentReportRepository extends JpaRepository<AssignmentReport, Long> {

    Optional<AssignmentReport> findByAssignment(Assignment assignment);

    /**
     * Returns the IDs of the assignments whose reports a reviewer has closed.
     */
    @Query("select r.assignment.id from AssignmentReport r where r.closed.subject is not null")
    Set<Long> findAssignmentIdsOfClosedReports();
}
