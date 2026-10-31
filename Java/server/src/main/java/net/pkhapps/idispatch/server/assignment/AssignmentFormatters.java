package net.pkhapps.idispatch.server.assignment;

import net.pkhapps.idispatch.server.assignmenttype.AssignmentType;
import net.pkhapps.idispatch.server.ui.Formatters;

import java.util.Optional;

public final class AssignmentFormatters {
    private AssignmentFormatters() {
    }

    public static String formatAssignment(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        var code = Optional.ofNullable(assignment.getType()).map(AssignmentType::getCode).orElse("?");
        var municipality = Optional.ofNullable(assignment.getMunicipality()).map(m -> m.getName(Formatters.currentLocale())).orElse("?");
        var address = Optional.ofNullable(assignment.getAddress()).orElse("?");
        return String.format("%s%s - %s - %s (%d)", code, assignment.getUrgency(), municipality, address, assignment.getId());
    }

    public static String assignmentUrgencyClassName(AssignmentUrgency assignmentUrgency) {
        return assignmentUrgency == null ? null : String.format("urgency-%s", assignmentUrgency.name().toLowerCase());
    }
}
