package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.AssignmentType;
import net.pkhapps.idispatch.server.entity.Municipality;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * Which closed assignments to list. Each {@code null} criterion matches every assignment. The dates are in the server's
 * time zone, and both ends of the range are included.
 */
public record ClosedAssignmentFilter(
        @Nullable LocalDate closedFrom,
        @Nullable LocalDate closedTo,
        @Nullable Municipality municipality,
        @Nullable AssignmentType type,
        @Nullable Boolean reportClosed
) {
}
