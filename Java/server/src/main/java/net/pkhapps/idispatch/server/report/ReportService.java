package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeListItem;
import net.pkhapps.idispatch.server.municipality.MunicipalityListItem;
import net.pkhapps.idispatch.server.shared.UpdateResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReportService {

    /**
     * Finds the report of the closed assignment with the specified ID. If nobody has filled in what happened on scene
     * yet, the report contains an empty, unsaved {@link AssignmentReport}.
     */
    Optional<ReportDTO> findReportByAssignmentId(long assignmentId);

    /**
     * Finds one page of the closed assignments that match the filter, sorted by the page request by the {@code SORT_}
     * properties of {@link ClosedAssignmentListItem}. A report is open until a reviewer closes it, including when
     * nobody has written anything in it yet.
     */
    List<ClosedAssignmentListItem> findClosedAssignments(ClosedAssignmentFilter filter, Pageable pageable);

    /**
     * Counts the closed assignments that match the filter.
     */
    int countClosedAssignments(ClosedAssignmentFilter filter);

    /**
     * Returns the municipalities that at least one closed assignment is in, deleted or not, in no particular order.
     */
    List<MunicipalityListItem> findMunicipalitiesOfClosedAssignments();

    /**
     * Returns the types that at least one closed assignment has, deleted or not, by code.
     */
    List<AssignmentTypeListItem> findTypesOfClosedAssignments();

    /**
     * Saves what a writer filled in, recording the edit on the parts they changed, and returns:
     * <ul>
     * <li>{@link UpdateResult.Success} with the saved report if the operation was successful.</li>
     * <li>{@link UpdateResult.Conflict} with the other user's report if another user saved the report after the writer
     * read it.</li>
     * <li>{@link UpdateResult.NoChange} with the closed report if a reviewer has closed it; closed reports can't be
     * changed.</li>
     * </ul>
     *
     * @param seen the report as the writer read it, whose version must still be the stored one
     */
    UpdateResult<AssignmentReport> saveReport(AssignmentReport seen, ReportDraft draft);

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
