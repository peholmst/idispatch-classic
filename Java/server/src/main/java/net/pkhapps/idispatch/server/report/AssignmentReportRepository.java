package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Repository of {@link AssignmentReport}s.
 */
interface AssignmentReportRepository extends JpaRepository<AssignmentReport, Long> {

    Optional<AssignmentReport> findByAssignment(Assignment assignment);

    /**
     * Returns the IDs of those of the specified assignments whose reports a reviewer has closed.
     */
    @Query("select r.assignment.id from AssignmentReport r where r.closed.subject is not null and r.assignment.id in :assignmentIds")
    Set<Long> findAssignmentIdsOfClosedReports(Collection<Long> assignmentIds);
}
