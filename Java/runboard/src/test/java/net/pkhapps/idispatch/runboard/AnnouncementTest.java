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

        assertEquals("Tehtävä 203 B. Rakennuspalo: keskisuuri. Parainen, Kauppiaskatu 5. Savua näkyy ikkunoista. "
                + "Yksiköt: P A R 11, R V S 201.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void swedishNotification_isReadWithSwedishPhrases() {
        var notification = new Notification(Map.of(
                "language", "sv",
                "assignment_type_code", "203",
                "urgency", "B",
                "municipality", "Pargas",
                "resources", List.of("RVS201")));

        assertEquals("Uppdrag 203 B. Pargas. Enheter: R V S 201.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void unknownLanguage_isReadWithoutPhrases() {
        var notification = new Notification(Map.of(
                "assignment_type_code", "203",
                "address", "Storgatan 5",
                "resources", List.of("RVS201")));

        assertEquals("203. Storgatan 5. R V S 201.", Announcement.text(notification, notification.getLanguage()));
    }

    @Test
    void givenLanguage_decidesThePhrases() {
        var notification = new Notification(Map.of("assignment_type_code", "203"));

        assertEquals("Tehtävä 203.", Announcement.text(notification, "fi"));
    }

    @Test
    void callSigns_haveTheirLettersSpelledOut() {
        assertEquals("R V S 201", Announcement.spellCallSign("RVS201"));
        assertEquals("P A R", Announcement.spellCallSign("PAR"));
        assertEquals("Ä Ö 3", Announcement.spellCallSign("ÄÖ3"));
        assertEquals("rvs-201", Announcement.spellCallSign("rvs-201"));
    }
}
