package net.pkhapps.idispatch.runboard.speech;

/**
 * Creates a {@link SpeechEngine}. Implementations are found with {@link java.util.ServiceLoader} and must have a
 * public no-argument constructor.
 */
public interface SpeechEngineProvider {

    /**
     * Returns the name that selects this engine in {@code idispatch.speech.engine}, such as {@code piper}. Its own
     * settings are read from properties starting with {@code idispatch.speech.<name>.}.
     */
    String name();

    /**
     * Creates the engine, loading whatever it needs up front. Called once, on the audio thread, when the runboard
     * starts. If this throws, the runboard logs the error and plays the alarm without speech.
     *
     * @param settings the engine's own settings
     */
    SpeechEngine create(SpeechSettings settings) throws Exception;
}
