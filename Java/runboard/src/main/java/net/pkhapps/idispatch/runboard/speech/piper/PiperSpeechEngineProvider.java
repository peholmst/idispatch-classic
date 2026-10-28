package net.pkhapps.idispatch.runboard.speech.piper;

import net.pkhapps.idispatch.runboard.speech.SpeechEngine;
import net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;

/**
 * Provides the {@code piper} engine, which speaks with <a href="https://github.com/OHF-Voice/piper1-gpl">Piper</a>
 * voices through piper-jni.
 * <p>
 * Settings: {@code voice.<language>} is the path of the voice's {@code .onnx} file for each language, with its
 * {@code .onnx.json} file next to it.
 */
public class PiperSpeechEngineProvider implements SpeechEngineProvider {

    @Override
    public String name() {
        return "piper";
    }

    @Override
    public SpeechEngine create(SpeechSettings settings) throws Exception {
        return new PiperSpeechEngine(settings);
    }
}
