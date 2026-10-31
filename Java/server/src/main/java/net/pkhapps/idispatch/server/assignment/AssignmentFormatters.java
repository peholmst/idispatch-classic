package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class AssignmentFormatters {
    private AssignmentFormatters() {
    }

    public static @Nullable String formatAssignment(@Nullable Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        var locale = Formatters.currentLocale();
        var code = Objects.requireNonNullElse(assignment.getTypeCode(), "?");
        var municipality = Objects.requireNonNullElse(assignment.getMunicipalityName(locale), "?");
        return String.format("%s%s - %s - %s (%d)", code, assignment.getUrgency(), municipality,
                assignment.getAddress(), assignment.getId());
    }

    /**
     * Formats the assignment's type as types are shown everywhere, or returns {@code null} if it has none.
     */
    public static @Nullable String formatAssignmentType(Assignment assignment) {
        var code = assignment.getTypeCode();
        return code == null ? null
                : AssignmentTypeFormatters.formatAssignmentType(code,
                assignment.getTypeDescription(Formatters.currentLocale()));
    }

    /**
     * Returns the name of the assignment's municipality in the language of the current UI, or {@code null} if it has
     * none.
     */
    public static @Nullable String formatMunicipality(Assignment assignment) {
        return assignment.getMunicipalityName(Formatters.currentLocale());
    }

    public static @Nullable String assignmentUrgencyClassName(@Nullable AssignmentUrgency assignmentUrgency) {
        return assignmentUrgency == null ? null : String.format("urgency-%s", assignmentUrgency.name().toLowerCase());
    }
}
