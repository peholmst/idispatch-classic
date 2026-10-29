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
                "assignment_type_code", "203",
                "urgency", "B",
                "assignment_type_descr", "Rakennuspalo: keskisuuri",
                "municipality", "Parainen",
                "address", "Kauppiaskatu 5",
                "description", "Savua näkyy ikkunoista.",
                "resources", List.of("RVS201", "PAR11")));

        assertEquals("Tehtävä 203 Bertta. Rakennuspalo: keskisuuri. Parainen, Kauppiaskatu 5. Savua näkyy ikkunoista. "
                + "Yksiköt: P A R 1 1, R V S 2 0 1.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void swedishNotification_isReadWithSwedishPhrases() {
        var notification = new Notification(Map.of(
                "language", "sv",
                "assignment_type_code", "203",
                "urgency", "B",
                "municipality", "Pargas",
                "resources", List.of("RVS201")));

        assertEquals("Uppdrag 203 Berta. Pargas. Enheter: R V S 2 0 1.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void unknownLanguage_isReadWithoutPhrases() {
        var notification = new Notification(Map.of(
                "assignment_type_code", "203",
                "address", "Storgatan 5",
                "resources", List.of("RVS201")));

        assertEquals("203. Storgatan 5. R V S 2 0 1.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void givenLanguage_decidesThePhrases() {
        var notification = new Notification(Map.of("assignment_type_code", "203"));

        assertEquals("Tehtävä 203.", Announcement.text(notification, "fi"));
    }

    @Test
    void urgency_isReadWithTheRadioWordOfTheLanguage() {
        assertEquals("Tehtävä 1 Aarne.", urgencyText("fi", "A"));
        assertEquals("Tehtävä 1 Celsius.", urgencyText("fi", "C"));
        assertEquals("Tehtävä 1 David.", urgencyText("fi", "D"));
        assertEquals("Tehtävä 1 Nilo.", urgencyText("fi", "N"));
        assertEquals("Uppdrag 1 Arne.", urgencyText("sv", "A"));
        assertEquals("Uppdrag 1 Niklas.", urgencyText("sv", "N"));
        assertEquals("1 B.", urgencyText("", "B"));
        assertEquals("Tehtävä 1 X.", urgencyText("fi", "X"));
    }

    private static String urgencyText(String language, String urgency) {
        return Announcement.text(new Notification(Map.of("assignment_type_code", "1", "urgency", urgency)), language);
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
