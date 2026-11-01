package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.output.OutputSettings;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

class Configuration extends net.pkhapps.idispatch.runboard.client.Configuration {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public boolean isUndecorated() {
        return Boolean.parseBoolean(getProperty("idispatch.undecorated", "true"));
    }

    public Boolean isLowResolution() {
        return Boolean.parseBoolean(getProperty("idispatch.lowres", "false"));
    }

    /**
     * Returns whether the mouse cursor is hidden over the runboard's windows, as on a kiosk without a mouse. Default:
     * false.
     */
    public boolean isCursorHidden() {
        return Boolean.parseBoolean(getProperty("idispatch.hideCursor", "false").strip());
    }

    /**
     * Returns how long each notification is shown on the screen, counted from when it was sent. The flashing light is
     * on for as long as a notification is shown. Default: 3 minutes.
     */
    public Duration getNotificationShownFor() {
        return Duration.ofSeconds(getInt("idispatch.notification.shownFor", 180, 1));
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

    /**
     * Returns the silence between the phrases of a notification that is read out loud: between the units dispatched,
     * and before the assignment and the address. Default: 500 ms.
     */
    public Duration getSpeechPhrasePause() {
        return Duration.ofMillis(getInt("idispatch.speech.phrasePause", 500, 0));
    }

    /**
     * Returns the gain of the alarm and the error beep in decibels, at most 0. The sound files are close to full scale,
     * far louder than speech even after it has been normalized, so they are attenuated to suit the speech. Default:
     * -12 dB, which puts the alarm just above the speech.
     */
    public double getAlarmVolume() {
        final String property = "idispatch.alarm.volume";
        final double defaultValue = -12;
        final String value = getProperty(property, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            final double result = Double.parseDouble(value.strip());
            if (result <= 0) {
                return result;
            }
        } catch (NumberFormatException ignored) {
            // Falls through to the warning below
        }
        logger.warn("Invalid value '{}' for {}, must be a number of decibels of at most 0; using {}", value, property,
                defaultValue);
        return defaultValue;
    }

    /**
     * Returns the name of the driver that switches the amplifier and the flashing light, or {@link Outputs#NONE} (the
     * default) for none.
     */
    public String getOutputDriver() {
        return getProperty("idispatch.output.driver", Outputs.NONE).strip();
    }

    /**
     * Returns the settings of the {@linkplain #getOutputDriver() output driver}: the properties starting with
     * {@code idispatch.output.<driver name>.}.
     */
    public OutputSettings getOutputSettings() {
        final String prefix = "idispatch.output." + getOutputDriver() + ".";
        return name -> Optional.ofNullable(getProperty(prefix + name, null))
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }

    /**
     * Returns how long to wait after switching the amplifier on before playing sound. Default: 500 ms.
     */
    public Duration getAmplifierWarmUp() {
        return Duration.ofMillis(getInt("idispatch.output.amplifierWarmUp", 500, 0));
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
