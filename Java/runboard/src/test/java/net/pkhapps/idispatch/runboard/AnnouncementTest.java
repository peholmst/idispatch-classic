package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

        assertEquals("Hälytysilmoitus: P A R 1 1, R V S 9 1 1. Tehtävä: 1 0 3 Bertta. Kauppiaskatu 1, Parainen.",
                Announcement.text(notification, notification.getLanguage()));
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

        assertEquals("Larmmeddelande: R V S 9 1 1. Uppdrag: 1 0 3 Berta. Köpmansgatan 1, Pargas.",
                Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void unknownLanguage_isReadWithoutPhrases() {
        var notification = new Notification(Map.of(
                "assignment_type_code", "103",
                "address", "Storgatan 1",
                "resources", List.of("RVS911")));

        assertEquals("R V S 9 1 1. 1 0 3. Storgatan 1.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void missingParts_areLeftOut() {
        var notification = new Notification(Map.of("assignment_type_code", "103"));

        assertEquals("Tehtävä: 1 0 3.", Announcement.text(notification, "fi"));
    }

    @Test
    void urgency_isReadWithTheRadioWordOfTheLanguage() {
        assertEquals("Tehtävä: 1 Aarne.", urgencyText("fi", "A"));
        assertEquals("Tehtävä: 1 Celsius.", urgencyText("fi", "C"));
        assertEquals("Tehtävä: 1 David.", urgencyText("fi", "D"));
        assertEquals("Tehtävä: 1 Nilo.", urgencyText("fi", "N"));
        assertEquals("Uppdrag: 1 Arne.", urgencyText("sv", "A"));
        assertEquals("Uppdrag: 1 Niklas.", urgencyText("sv", "N"));
        assertEquals("1 B.", urgencyText("", "B"));
        assertEquals("Tehtävä: 1 X.", urgencyText("fi", "X"));
    }

    private static String urgencyText(String language, String urgency) {
        return Announcement.text(new Notification(Map.of("assignment_type_code", "1", "urgency", urgency)), language);
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
