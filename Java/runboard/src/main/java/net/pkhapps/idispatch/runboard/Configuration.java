package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

public class Configuration extends net.pkhapps.idispatch.runboard.client.Configuration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public boolean isUndecorated() {
        return Boolean.parseBoolean(getProperty("idispatch.undecorated", "true"));
    }

    public Boolean isLowResolution() {
        return Boolean.parseBoolean(getProperty("idispatch.lowres", "false"));
    }

    /**
     * Returns the name of the speech engine that reads notifications out loud, or {@link Speech#NONE} for no speech.
     */
    public String getSpeechEngine() {
        return getProperty("idispatch.speech.engine", "piper").strip();
    }

    /**
     * Returns the language to read notifications in when the server does not say, or an empty string for none.
     */
    public String getSpeechDefaultLanguage() {
        return getProperty("idispatch.speech.defaultLanguage", "").strip();
    }

    /**
     * Returns the settings of the {@linkplain #getSpeechEngine() speech engine}: the properties starting with
     * {@code idispatch.speech.<engine name>.}.
     */
    public SpeechSettings getSpeechSettings() {
        final String prefix = "idispatch.speech." + getSpeechEngine() + ".";
        return name -> Optional.ofNullable(getProperty(prefix + name, null))
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    /**
     * Returns how alerts play out. Missing or invalid values fall back to the defaults: the alarm plays 9 times, or 3
     * times before the notifications are read out loud twice, each reading after a second of silence.
     */
    public AlertTimings getAlertTimings() {
        final int alarmPlays = getInt("idispatch.alarm.plays", 9, 1);
        final int alarmPlaysBeforeSpeech = getInt("idispatch.alarm.playsBeforeSpeech", 3, 1);
        if (alarmPlaysBeforeSpeech > alarmPlays) {
            logger.warn("idispatch.alarm.playsBeforeSpeech ({}) is more than idispatch.alarm.plays ({}), using {}",
                    alarmPlaysBeforeSpeech, alarmPlays, alarmPlays);
        }
        return new AlertTimings(alarmPlays,
                Math.min(alarmPlaysBeforeSpeech, alarmPlays),
                getInt("idispatch.speech.repeats", 2, 1),
                Duration.ofMillis(getInt("idispatch.speech.pause", 1000, 0)));
    }

    private int getInt(String property, int defaultValue, int minimum) {
        final String value = getProperty(property, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            final int result = Integer.parseInt(value.strip());
            if (result >= minimum) {
                return result;
            }
        } catch (NumberFormatException ignored) {
            // Falls through to the warning below
        }
        logger.warn("Invalid value '{}' for {}, must be a whole number of at least {}; using {}", value, property,
                minimum, defaultValue);
        return defaultValue;
    }
}
