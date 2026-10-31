package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentTypeFormatters;
import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public final class AssignmentFormatters {
    private AssignmentFormatters() {
    }

    public static @Nullable String formatAssignment(@Nullable AssignmentListItem assignment) {
        if (assignment == null) {
            return null;
        }
        var locale = Formatters.currentLocale();
        var code = Objects.requireNonNullElse(assignment.typeCode(), "?");
        var municipality = Objects.requireNonNullElse(assignment.municipalityName(locale), "?");
        return String.format("%s%s - %s - %s (%d)", code, assignment.urgency(), municipality, assignment.address(),
                assignment.id().value());
    }

    /**
     * Formats the assignment's type as types are shown everywhere, or returns {@code null} if it has none.
     */
    public static @Nullable String formatAssignmentType(AssignmentListItem assignment) {
        var code = assignment.typeCode();
        return code == null ? null
                : AssignmentTypeFormatters.formatAssignmentType(code,
                assignment.typeDescription(Formatters.currentLocale()));
    }

    /**
     * Returns the name of the assignment's municipality in the language of the current UI, or {@code null} if it has
     * none.
     */
    public static @Nullable String formatMunicipality(AssignmentListItem assignment) {
        return assignment.municipalityName(Formatters.currentLocale());
    }

    public static @Nullable String assignmentUrgencyClassName(@Nullable AssignmentUrgency assignmentUrgency) {
        return assignmentUrgency == null ? null : String.format("urgency-%s", assignmentUrgency.name().toLowerCase());
    }
}
