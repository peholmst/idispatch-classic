package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechSettings;

import java.util.Optional;

public class Configuration extends net.pkhapps.idispatch.runboard.client.Configuration {

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
     * Returns the settings of the {@linkplain #getSpeechEngine() speech engine}: the properties starting with
     * {@code idispatch.speech.<engine name>.}.
     */
    public SpeechSettings getSpeechSettings() {
        final String prefix = "idispatch.speech." + getSpeechEngine() + ".";
        return name -> Optional.ofNullable(getProperty(prefix + name, null))
                .map(String::strip)
                .filter(value -> !value.isEmpty());
    }
}
