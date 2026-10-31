package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeId;
import net.pkhapps.idispatch.server.municipality.MunicipalityId;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Which closed assignments to list. Each {@code null} criterion matches every assignment. The dates are in the server's
 * time zone, and both ends of the range are included.
 */
public record ClosedAssignmentFilter(
        @Nullable LocalDate closedFrom,
        @Nullable LocalDate closedTo,
        @Nullable MunicipalityId municipality,
        @Nullable AssignmentTypeId type,
        @Nullable Boolean reportClosed
) {
}
