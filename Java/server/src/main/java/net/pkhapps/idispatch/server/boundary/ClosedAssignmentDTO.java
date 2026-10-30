package net.pkhapps.idispatch.server.boundary;

import net.pkhapps.idispatch.server.entity.Assignment;
import net.pkhapps.idispatch.server.entity.repository.AssignmentSpecifications;

import java.io.Serializable;

/**
 * A closed assignment in the list of reports, and whether a reviewer has closed its report.
 */
public record ClosedAssignmentDTO(Assignment assignment, boolean reportClosed) implements Serializable {

    /**
     * Sort property for {@link #reportClosed()}. The other sort properties are those of {@link Assignment}.
     */
    public static final String SORT_REPORT_CLOSED = AssignmentSpecifications.REPORT_CLOSED;
}
