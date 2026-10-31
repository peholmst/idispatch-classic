package net.pkhapps.idispatch.server.report;

import net.pkhapps.idispatch.server.assignment.AssignmentId;
import net.pkhapps.idispatch.server.assignment.AssignmentUrgency;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;

/**
 * A closed assignment as the reports list it, flattened to what the list shows. The type and the municipality are those
 * the assignment has now, in both languages, and are {@code null} when it has none.
 *
 * @param reportClosed whether a reviewer has closed the assignment's report
 */
public record ClosedAssignmentListItem(
        AssignmentId id,
        Instant closed,
        AssignmentUrgency urgency,
        @Nullable String typeCode,
        @Nullable String typeDescriptionFi,
        @Nullable String typeDescriptionSv,
        @Nullable String municipalityNameFi,
        @Nullable String municipalityNameSv,
        String address,
        boolean reportClosed
) implements Serializable {

    // The properties the list can be sorted by
    public static final String SORT_NUMBER = "number";
    public static final String SORT_CLOSED = "closed";
    public static final String SORT_URGENCY = "urgency";
    public static final String SORT_TYPE_CODE = "typeCode";
    public static final String SORT_TYPE_DESCRIPTION_FI = "typeDescriptionFi";
    public static final String SORT_TYPE_DESCRIPTION_SV = "typeDescriptionSv";
    public static final String SORT_MUNICIPALITY_NAME_FI = "municipalityNameFi";
    public static final String SORT_MUNICIPALITY_NAME_SV = "municipalityNameSv";
    public static final String SORT_ADDRESS = "address";
    /**
     * Open reports come first in ascending order.
     */
    public static final String SORT_REPORT_CLOSED = "reportClosed";
}
