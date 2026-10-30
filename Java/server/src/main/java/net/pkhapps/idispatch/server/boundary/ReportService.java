package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.AssignmentReport;
import net.pkhapps.idispatch.server.entity.ValidationFailedException;
import net.pkhapps.idispatch.server.util.UpdateResult;

import java.util.Optional;

public interface ReportService {

    /**
     * Finds the report of the closed assignment with the specified ID. If nobody has filled in what happened on scene
     * yet, the report contains a new, unsaved {@link AssignmentReport}.
     */
    Optional<ReportDTO> findReportByAssignmentId(long assignmentId);

    /**
     * Validates and saves what happened on scene, and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the saved report if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} with the other user's report if another user saved the report first.</li>
     * </ul>
     */
    UpdateResult<AssignmentReport> saveReport(AssignmentReport report) throws ValidationFailedException;
}
