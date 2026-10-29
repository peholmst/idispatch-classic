package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds the text that is read out loud for a notification.
 */
final class Announcement {

    private record Phrases(String assignment, String resources) {
    }

    private static final Map<String, Phrases> PHRASES = Map.of(
            "fi", new Phrases("Tehtävä", "Yksiköt"),
            "sv", new Phrases("Uppdrag", "Enheter"));
    private static final Phrases NO_PHRASES = new Phrases("", "");
    private static final Pattern CALL_SIGN = Pattern.compile("(\\p{Lu}+)(\\d*)");

    private Announcement() {
    }

    /**
     * Returns the text to read for the notification in the given language, which decides the words added around the
     * notification's own text.
     */
    static String text(Notification notification, String language) {
        final Phrases phrases = PHRASES.getOrDefault(language, NO_PHRASES);
        final List<String> sentences = new ArrayList<>();
        sentences.add(join(" ", phrases.assignment(), notification.getAssignmentTypeCode(), notification.getUrgency()));
        sentences.add(notification.getAssignmentTypeDescription());
        sentences.add(join(", ", notification.getMunicipality(), notification.getAddress()));
        sentences.add(notification.getDescription());
        final String resources = notification.getResources().stream()
                .sorted()
                .map(Announcement::spellCallSign)
                .collect(Collectors.joining(", "));
        if (!resources.isEmpty()) {
            sentences.add(phrases.resources().isEmpty() ? resources : phrases.resources() + ": " + resources);
        }
        return sentences.stream()
                .map(String::strip)
                .filter(sentence -> !sentence.isEmpty())
                .map(sentence -> sentence.endsWith(".") ? sentence : sentence + ".")
                .collect(Collectors.joining(" "));
    }

    /**
     * Spells out the letters of a call sign such as {@code RVS201}, which would otherwise be read as a word.
     */
    static String spellCallSign(String callSign) {
        final var matcher = CALL_SIGN.matcher(callSign);
        if (!matcher.matches()) {
            return callSign;
        }
        return join(" ", String.join(" ", matcher.group(1).split("")), matcher.group(2));
    }

    private static String join(String delimiter, String... parts) {
        return Arrays.stream(parts)
                .map(String::strip)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(delimiter));
    }
}
