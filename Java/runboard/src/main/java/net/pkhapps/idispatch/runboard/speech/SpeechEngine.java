package net.pkhapps.idispatch.runboard.speech;

import java.util.Optional;

/**
 * Turns text into speech. An engine is only ever used from the runboard's audio thread, so it does not have to be
 * thread safe.
 */
public interface SpeechEngine extends AutoCloseable {

    /**
     * Returns whether the engine can speak the given language. Called before the alarm starts, to decide how long it
     * plays, so it must answer quickly: check the configuration, don't load anything.
     *
     * @param language an ISO 639-1 code such as {@code fi} or {@code sv}, or an empty string if the server did not
     *                 say
     */
    boolean supportsLanguage(String language);

    /**
     * Speaks the text in the given language. Called while the alarm plays; the result is played when the alarm ends.
     *
     * @return the speech, or an empty optional if the engine cannot speak the language
     * @throws Exception if synthesis fails; the runboard logs it and carries on without speech
     */
    Optional<SpeechAudio> synthesize(String language, String text) throws Exception;

    /**
     * Releases the engine's resources. The default does nothing.
     */
    @Override
    default void close() throws Exception {
    }
}
