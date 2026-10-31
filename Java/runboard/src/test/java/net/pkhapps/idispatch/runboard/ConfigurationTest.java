package net.pkhapps.idispatch.runboard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Sets system properties, which the configuration falls back to when there is no {@code runboard.properties}.
 */
class ConfigurationTest {

    private static final List<String> TIMING_PROPERTIES = List.of("idispatch.alarm.plays",
            "idispatch.alarm.playsBeforeSpeech", "idispatch.speech.repeats", "idispatch.speech.pause",
            "idispatch.notification.shownFor");

    @AfterEach
    void clearProperties() {
        TIMING_PROPERTIES.forEach(System::clearProperty);
    }

    @Test
    void alertTimings_haveDefaults() {
        assertEquals(new AlertTimings(9, 3, 2, Duration.ofSeconds(1)), new Configuration().getAlertTimings());
    }

    @Test
    void alertTimings_areRead() {
        System.setProperty("idispatch.alarm.plays", "6");
        System.setProperty("idispatch.alarm.playsBeforeSpeech", "2");
        System.setProperty("idispatch.speech.repeats", "3");
        System.setProperty("idispatch.speech.pause", " 500 ");

        assertEquals(new AlertTimings(6, 2, 3, Duration.ofMillis(500)), new Configuration().getAlertTimings());
    }

    @Test
    void invalidAlertTimings_fallBackToDefaults() {
        System.setProperty("idispatch.alarm.plays", "0");
        System.setProperty("idispatch.alarm.playsBeforeSpeech", "two");
        System.setProperty("idispatch.speech.repeats", "-1");
        System.setProperty("idispatch.speech.pause", "1.5");

        assertEquals(new AlertTimings(9, 3, 2, Duration.ofSeconds(1)), new Configuration().getAlertTimings());
    }

    @Test
    void alarmPlaysBeforeSpeech_isAtMostAlarmPlays() {
        System.setProperty("idispatch.alarm.plays", "2");
        System.setProperty("idispatch.alarm.playsBeforeSpeech", "5");

        assertEquals(2, new Configuration().getAlertTimings().alarmPlaysBeforeSpeech());
    }

    @Test
    void notificationShownFor_defaultsToThreeMinutes() {
        assertEquals(Duration.ofMinutes(3), new Configuration().getNotificationShownFor());
    }

    @Test
    void notificationShownFor_isRead() {
        System.setProperty("idispatch.notification.shownFor", " 300 ");

        assertEquals(Duration.ofMinutes(5), new Configuration().getNotificationShownFor());
    }

    @Test
    void invalidNotificationShownFor_fallsBackToDefault() {
        System.setProperty("idispatch.notification.shownFor", "0");

        assertEquals(Duration.ofMinutes(3), new Configuration().getNotificationShownFor());
    }
}
