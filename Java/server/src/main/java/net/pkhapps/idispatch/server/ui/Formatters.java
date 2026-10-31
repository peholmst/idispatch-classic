package net.pkhapps.idispatch.server.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;

import java.text.Collator;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
    public static Locale currentLocale() {
        var ui = UI.getCurrent();
        return ui == null ? null : ui.getLocale();
    }

    public static String formatBoolean(Boolean bool) {
        return bool == null ? null : I18NProvider.translate(bool ? "common.yes" : "common.no");
    }
}
