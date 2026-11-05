package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnnouncementTest {

    @Test
    void finnishNotification_isReadWithFinnishPhrases() {
        var notification = new Notification(Map.of(
                "language", "fi",
                "assignment_type_code", "103",
                "urgency", "B",
                "assignment_type_descr", "Rakennuspalo: keskisuuri",
                "municipality", "Parainen",
                "address", "Kauppiaskatu 1",
                "description", "Savua näkyy ikkunoista.",
                "resources", List.of("RVS911", "PAR11")));

        assertEquals(List.of("Hälytysilmoitus: P A R 1 1.", "R V S 9 1 1.", "Tehtävä: 1 0 3 Bertta.",
                        "Kauppiaskatu 1, Parainen."),
                Announcement.phrases(notification, notification.getLanguage(), 10));
    }

    @Test
    void swedishNotification_isReadWithSwedishPhrases() {
        var notification = new Notification(Map.of(
                "language", "sv",
                "assignment_type_code", "103",
                "urgency", "B",
                "municipality", "Pargas",
                "address", "Köpmansgatan 1",
                "resources", List.of("RVS911")));

        assertEquals(List.of("Larmmeddelande: R V S 9 1 1.", "Uppdrag: 1 0 3 Bertil.", "Köpmansgatan 1, Pargas."),
                Announcement.phrases(notification, notification.getLanguage(), 10));
    }

    @Test
    void unknownLanguage_isReadWithoutPhrases() {
        var notification = new Notification(Map.of(
                "assignment_type_code", "103",
                "address", "Storgatan 1",
                "resources", List.of("RVS911")));

        assertEquals(List.of("R V S 9 1 1.", "1 0 3.", "Storgatan 1."),
                Announcement.phrases(notification, notification.getLanguage(), 10));
    }

    @Test
    void finnishUnits_upToTheMaximum_areListed() {
        assertEquals(List.of("Hälytysilmoitus: R V S 1 1.", "R V S 1 2.", "R V S 1 3.", "Tehtävä: 1 0 3 Bertta.",
                        "Kauppiaskatu 1, Parainen."),
                Announcement.phrases(notificationWithUnits("fi", 3), "fi", 3));
    }

    @Test
    void finnishUnits_overTheMaximum_areReadAsTheirNumber() {
        assertEquals(List.of("Hälytysilmoitus: 4 yksikköä.", "Tehtävä: 1 0 3 Bertta.", "Kauppiaskatu 1, Parainen."),
                Announcement.phrases(notificationWithUnits("fi", 4), "fi", 3));
    }

    @Test
    void swedishUnits_upToTheMaximum_areListed() {
        assertEquals(List.of("Larmmeddelande: R V S 1 1.", "R V S 1 2.", "R V S 1 3.", "Uppdrag: 1 0 3 Bertil.",
                        "Kauppiaskatu 1, Parainen."),
                Announcement.phrases(notificationWithUnits("sv", 3), "sv", 3));
    }

    @Test
    void swedishUnits_overTheMaximum_areReadAsTheirNumber() {
        assertEquals(List.of("Larmmeddelande: 12 enheter.", "Uppdrag: 1 0 3 Bertil.", "Kauppiaskatu 1, Parainen."),
                Announcement.phrases(notificationWithUnits("sv", 12), "sv", 10));
    }

    @Test
    void unitsInUnknownLanguage_overTheMaximum_areListed() {
        assertEquals(List.of("R V S 1 1.", "R V S 1 2.", "1 0 3 B.", "Kauppiaskatu 1, Parainen."),
                Announcement.phrases(notificationWithUnits("", 2), "", 1));
    }

    private static Notification notificationWithUnits(String language, int count) {
        return new Notification(Map.of(
                "language", language,
                "assignment_type_code", "103",
                "urgency", "B",
                "municipality", "Parainen",
                "address", "Kauppiaskatu 1",
                "resources", IntStream.rangeClosed(1, count).mapToObj(i -> "RVS" + (10 + i)).toList()));
    }

    @Test
    void missingParts_areLeftOut() {
        var notification = new Notification(Map.of("assignment_type_code", "103"));

        assertEquals(List.of("Tehtävä: 1 0 3."), Announcement.phrases(notification, "fi", 10));
    }

    @Test
    void urgency_isReadWithTheRadioWordOfTheLanguage() {
        assertEquals("Tehtävä: 1 Aarne.", urgencyText("fi", "A"));
        assertEquals("Tehtävä: 1 Celsius.", urgencyText("fi", "C"));
        assertEquals("Tehtävä: 1 David.", urgencyText("fi", "D"));
        assertEquals("Tehtävä: 1 Nilo.", urgencyText("fi", "N"));
        assertEquals("Uppdrag: 1 Adam.", urgencyText("sv", "A"));
        assertEquals("Uppdrag: 1 Bertil.", urgencyText("sv", "B"));
        assertEquals("Uppdrag: 1 Cesar.", urgencyText("sv", "C"));
        assertEquals("Uppdrag: 1 David.", urgencyText("sv", "D"));
        assertEquals("Uppdrag: 1 Niklas.", urgencyText("sv", "N"));
        assertEquals("1 B.", urgencyText("", "B"));
        assertEquals("Tehtävä: 1 X.", urgencyText("fi", "X"));
    }

    private static String urgencyText(String language, String urgency) {
        return String.join(" ", Announcement.phrases(
                new Notification(Map.of("assignment_type_code", "1", "urgency", urgency)), language, 10));
    }

    @Test
    void assignmentCodes_areReadDigitByDigit() {
        assertEquals("1 0 3", Announcement.spellDigits("103"));
        assertEquals("H 1 0 3", Announcement.spellDigits("H103"));
        assertEquals("1 0 3 B", Announcement.spellDigits("103B"));
        assertEquals("PALO", Announcement.spellDigits("PALO"));
        assertEquals("", Announcement.spellDigits(""));
    }

    @Test
    void callSigns_areSpelledOutLetterByLetterAndDigitByDigit() {
        assertEquals("R V S 9 1 1", Announcement.spellCallSign("RVS911"));
        assertEquals("P A R", Announcement.spellCallSign("PAR"));
        assertEquals("Ä Ö 3", Announcement.spellCallSign("ÄÖ3"));
        assertEquals("R 1 0", Announcement.spellCallSign("R10"));
        assertEquals("rvs-201", Announcement.spellCallSign("rvs-201"));
    }
}
