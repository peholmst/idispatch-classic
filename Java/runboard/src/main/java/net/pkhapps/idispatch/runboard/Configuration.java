package net.pkhapps.idispatch.runboard;

import java.nio.file.Path;
import java.util.Optional;

public class Configuration extends net.pkhapps.idispatch.runboard.client.Configuration {

    public boolean isUndecorated() {
        return Boolean.parseBoolean(getProperty("idispatch.undecorated", "true"));
    }

    public Boolean isLowResolution() {
        return Boolean.parseBoolean(getProperty("idispatch.lowres", "false"));
    }

    /**
     * Returns the Piper voice model ({@code .onnx} file) to read notifications in the given language with, if one has
     * been configured. The voice's {@code .onnx.json} file must be next to it.
     */
    public Optional<Path> getVoiceModel(String language) {
        return Optional.ofNullable(getProperty("idispatch.voice." + language, null))
                .filter(value -> !value.isBlank())
                .map(Path::of);
    }
}
