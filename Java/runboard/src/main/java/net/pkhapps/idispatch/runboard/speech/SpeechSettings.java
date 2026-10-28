package net.pkhapps.idispatch.runboard.speech;

import java.util.Optional;

/**
 * The settings of one speech engine, read from properties starting with {@code idispatch.speech.<engine name>.} in
 * {@code runboard.properties} or the system properties.
 */
@FunctionalInterface
public interface SpeechSettings {

    /**
     * Returns the value of a setting, or an empty optional if it is missing or blank.
     *
     * @param name the setting's name without the engine prefix, such as {@code voice.fi} for
     *             {@code idispatch.speech.piper.voice.fi}
     */
    Optional<String> get(String name);
}
