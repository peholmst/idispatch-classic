package net.pkhapps.idispatch.server.ui.dws;

import net.pkhapps.idispatch.server.entity.*;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public final class Formatters {
    private Formatters() {
    }

    public static String formatDateTime(Instant instant) {
        return instant == null ? null : DateTimeFormatter
                .ofPattern("dd.MM.yyyy HH:mm:ss")
                .format(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    public static String formatAssignmentType(AssignmentType assignmentType) {
        return assignmentType == null ? null : assignmentType.getFormattedDescription();
    }

    public static String formatMunicipality(Municipality municipality) {
        return municipality == null ? null : municipality.getName();
    }

    public static String formatResource(Resource resource) {
        return resource == null ? null : resource.getCallSign();
    }

    public static String formatResourceState(ResourceState resourceState) {
        return resourceState == null ? null : StringUtils.capitalize(resourceState.name().toLowerCase().replace('_', ' '));
    }

    public static String formatAssignment(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        var code = Optional.ofNullable(assignment.getType()).map(AssignmentType::getCode).orElse("?");
        var municipality = Optional.ofNullable(assignment.getMunicipality()).map(Municipality::getName).orElse("?");
        var address = Optional.ofNullable(assignment.getAddress()).orElse("?");
        return String.format("%s%s - %s - %s (%d)", code, assignment.getUrgency(), municipality, address, assignment.getId());
    }

    public static String assignmentUrgencyClassName(AssignmentUrgency assignmentUrgency) {
        return assignmentUrgency == null ? null : String.format("urgency-%s", assignmentUrgency.name().toLowerCase());
    }

    public static String resourceStateClassName(ResourceState resourceState) {
        return resourceState == null ? null : String.format("resource-state-%s", resourceState.name().toLowerCase());
    }
}
