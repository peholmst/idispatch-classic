package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;
import net.pkhapps.idispatch.server.entity.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public final class Formatters {
    private Formatters() {
    }

    public static String formatDateTime(Instant instant) {
        return instant == null ? null : DateTimeFormatter
                .ofPattern("dd.MM.yyyy HH:mm:ss")
                .format(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    public static String formatTime(Instant instant) {
        return instant == null ? null : DateTimeFormatter
                .ofPattern("HH:mm:ss")
                .format(ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    public static String formatAssignmentType(AssignmentType assignmentType) {
        return assignmentType == null ? null : assignmentType.getFormattedDescription(currentLocale());
    }

    public static String formatResourceType(ResourceType resourceType) {
        return resourceType == null ? null : resourceType.getFormattedDescription(currentLocale());
    }

    public static String formatMunicipality(Municipality municipality) {
        return municipality == null ? null : municipality.getName(currentLocale());
    }

    public static String formatResource(Resource resource) {
        return resource == null ? null : resource.getCallSign();
    }

    public static String formatResourceState(ResourceState resourceState) {
        return resourceState == null ? null : I18NProvider.translate("resource-state." + resourceState.name());
    }

    public static String formatAssignment(Assignment assignment) {
        if (assignment == null) {
            return null;
        }
        var code = Optional.ofNullable(assignment.getType()).map(AssignmentType::getCode).orElse("?");
        var municipality = Optional.ofNullable(assignment.getMunicipality()).map(m -> m.getName(currentLocale())).orElse("?");
        var address = Optional.ofNullable(assignment.getAddress()).orElse("?");
        return String.format("%s%s - %s - %s (%d)", code, assignment.getUrgency(), municipality, address, assignment.getId());
    }

    public static String assignmentUrgencyClassName(AssignmentUrgency assignmentUrgency) {
        return assignmentUrgency == null ? null : String.format("urgency-%s", assignmentUrgency.name().toLowerCase());
    }

    public static String resourceStateClassName(ResourceState resourceState) {
        return resourceState == null ? null : String.format("resource-state-%s", resourceState.name().toLowerCase());
    }

    /**
     * Returns the items sorted by their labels, using the sorting rules of the language of the current UI.
     */
    public static <T> List<T> sortedByLabel(List<T> items, Function<T, String> label) {
        var locale = currentLocale();
        var collator = Collator.getInstance(locale == null ? Locale.getDefault() : locale);
        return items.stream().sorted(Comparator.comparing(label, collator)).toList();
    }

    /**
     * Returns the locale of the current UI, which picks the language of the bilingual names and descriptions.
     */
    private static Locale currentLocale() {
        var ui = UI.getCurrent();
        return ui == null ? null : ui.getLocale();
    }

    public static String formatBoolean(Boolean bool) {
        return bool == null ? null : I18NProvider.translate(bool ? "common.yes" : "common.no");
    }
}
