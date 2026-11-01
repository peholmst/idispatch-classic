package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.ui.Formatters;
import org.jspecify.annotations.Nullable;

public final class AssignmentTypeFormatters {
    private AssignmentTypeFormatters() {
    }

    public static @Nullable String formatAssignmentType(@Nullable AssignmentTypeListItem assignmentType) {
        return assignmentType == null ? null
                : formatAssignmentType(assignmentType.code(), assignmentType.description(Formatters.currentLocale()));
    }

    /**
     * Formats a type as it is shown everywhere: its code, followed by its description.
     */
    public static String formatAssignmentType(String code, String description) {
        return code + " - " + description;
    }
}
