package net.pkhapps.idispatch.server.assignmenttype;

import net.pkhapps.idispatch.server.ui.Formatters;

public final class AssignmentTypeFormatters {
    private AssignmentTypeFormatters() {
    }

    public static String formatAssignmentType(AssignmentType assignmentType) {
        return assignmentType == null ? null : assignmentType.getFormattedDescription(Formatters.currentLocale());
    }
}
