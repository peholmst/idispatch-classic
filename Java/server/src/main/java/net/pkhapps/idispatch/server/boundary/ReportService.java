package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.util.UpdateResult;

import java.util.Optional;
import java.util.Set;

public interface ReportService {

    /**
     * Finds the report of the closed assignment with the specified ID. If nobody has filled in what happened on scene
     * yet, the report contains a new, unsaved {@link AssignmentReport}.
     */
    Optional<ReportDTO> findReportByAssignmentId(long assignmentId);

    /**
     * Returns the IDs of the assignments whose reports a reviewer has closed. The reports of all other closed
     * assignments are open, including those that nobody has written anything in yet.
     */
    Set<Long> findAssignmentIdsWithClosedReports();

    /**
     * Validates and saves what happened on scene, and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the saved report if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} with the other user's report if another user saved the report first.</li>
     * <li>{@link UpdateResult.NoChange} with the closed report if a reviewer has closed it; closed reports can't be
     * changed.</li>
     * </ul>
     */
    UpdateResult<AssignmentReport> saveReport(AssignmentReport report) throws ValidationFailedException;

    /**
     * Closes the report after a reviewer has checked it, so that nobody can change it. The report must be the version
     * the reviewer saw, so that nobody closes changes they haven't checked. Returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the closed report if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} with the current report if it has changed since the reviewer read it.</li>
     * </ul>
     */
    UpdateResult<AssignmentReport> closeReport(AssignmentReport report);

    /**
     * Re-opens a closed report, so that it can be changed again. Returns the same results as
     * {@link #closeReport(AssignmentReport)}.
     */
    UpdateResult<AssignmentReport> reopenReport(AssignmentReport report);
}
