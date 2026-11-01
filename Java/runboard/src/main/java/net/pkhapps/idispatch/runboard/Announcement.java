package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds the phrases that are read out loud for a notification: the units dispatched, the assignment code and
 * urgency, and the address. The assignment type's description and the free-text description are left out, as they are shown on
 * the screen.
 */
final class Announcement {

    /**
     * @param alert     the label of the units dispatched, which opens the announcement
     * @param urgencies the words that urgency letters are read as, the way they are said over the radio
     */
    private record Phrases(String alert, String assignment, Map<String, String> urgencies) {
    }

    private static final Map<String, Phrases> PHRASES = Map.of(
            "fi", new Phrases("Hälytysilmoitus", "Tehtävä",
                    Map.of("A", "Aarne", "B", "Bertta", "C", "Celsius", "D", "David", "N", "Nilo")),
            "sv", new Phrases("Larmmeddelande", "Uppdrag",
                    Map.of("A", "Arne", "B", "Berta", "C", "Celsius", "D", "David", "N", "Niklas")));
    private static final Phrases NO_PHRASES = new Phrases("", "", Map.of());
    private static final Pattern CALL_SIGN = Pattern.compile("(\\p{Lu}+)(\\d*)");
    private static final Pattern DIGIT_BOUNDARY = Pattern.compile("(?<=\\d)(?=\\S)|(?<=\\S)(?=\\d)");

    private Announcement() {
    }

    /**
     * Returns the languages that notifications are read in: the ones with words of their own around the text.
     */
    static Set<String> languages() {
        return PHRASES.keySet();
    }

    /**
     * Returns the phrases to read for the notification in the given language, which decides the words added around the
     * notification's own text. Each unit dispatched is a phrase of its own, the first one after the alert label, so
     * that the runboard can pause between them; the assignment and the address follow, a phrase each. Every phrase
     * ends with a full stop.
     */
    static List<String> phrases(Notification notification, String language) {
        final Phrases phrases = PHRASES.getOrDefault(language, NO_PHRASES);
        final List<String> result = new ArrayList<>();
        final List<String> units = notification.getResources().stream()
                .sorted()
                .map(Announcement::spellCallSign)
                .toList();
        for (int i = 0; i < units.size(); i++) {
            result.add(i == 0 ? labelled(phrases.alert(), units.get(i)) : units.get(i));
        }
        final String urgency = notification.getUrgency();
        result.add(labelled(phrases.assignment(), join(" ", spellDigits(notification.getAssignmentTypeCode()),
                phrases.urgencies().getOrDefault(urgency, urgency))));
        result.add(join(", ", notification.getAddress(), notification.getMunicipality()));
        return result.stream()
                .map(String::strip)
                .filter(phrase -> !phrase.isEmpty())
                .map(phrase -> phrase.endsWith(".") ? phrase : phrase + ".")
                .toList();
    }

    /**
     * Spells out a call sign such as {@code RVS201} letter by letter and digit by digit, as {@code R V S 2 0 1}, which
     * would otherwise be read as a word and a number.
     */
    static String spellCallSign(String callSign) {
        final var matcher = CALL_SIGN.matcher(callSign);
        if (!matcher.matches()) {
            return callSign;
        }
        return join(" ", String.join(" ", matcher.group(1).split("")), String.join(" ", matcher.group(2).split("")));
    }

    /**
     * Separates every digit from the characters next to it, so that an assignment code such as {@code 103} is read
     * digit by digit, as {@code 1 0 3}, and not as a number.
     */
    static String spellDigits(String text) {
        return DIGIT_BOUNDARY.matcher(text).replaceAll(" ");
    }

    private static String labelled(String label, String text) {
        if (text.isBlank()) {
            return "";
        }
        return label.isEmpty() ? text : label + ": " + text;
    }

    private static String join(String delimiter, String... parts) {
        return Arrays.stream(parts)
                .map(String::strip)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(delimiter));
    }
}
