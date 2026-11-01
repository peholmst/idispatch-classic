package net.pkhapps.idispatch.runboard.speech;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Turns text into speech. The runboard creates, prepares, uses and closes an engine from one thread at a time, its
 * speech thread, so the engine does not have to be thread safe. The one exception is
 * {@link #supportsLanguage(String)}, which the audio thread calls, possibly while the engine is synthesizing.
 */
public interface SpeechEngine extends AutoCloseable {

    /**
     * Returns whether the engine can speak the given language. Called from the audio thread before the alarm starts,
     * to decide how long it plays, possibly while the speech thread is synthesizing. So it must answer quickly and
     * without touching what synthesis uses: check the configuration, don't load anything.
     *
     * @param language an ISO 639-1 code such as {@code fi} or {@code sv}, or an empty string if the server did not
     *                 say
     */
    boolean supportsLanguage(String language);

    /**
     * Gets ready to speak the given language, so that the first notification in it is not slower than the rest: loads
     * the voice, and synthesizes something short if the first synthesis is slower than the ones after it. Called when
     * the runboard starts, for each language that notifications can come in and that the engine
     * {@linkplain #supportsLanguage(String) supports}. The default does nothing.
     *
     * @throws Exception if the language cannot be prepared; the runboard logs it and tries again when it is needed
     */
    default void prepare(String language) throws Exception {
    }

    /**
     * Speaks the text in the given language. The result is played when it is complete; engines that can produce the
     * speech in parts should also override {@link #synthesize(String, String, Consumer)}.
     *
     * @return the speech, or an empty optional if the engine cannot speak the language
     * @throws Exception if synthesis fails; the runboard logs it and carries on without speech
     */
    Optional<SpeechAudio> synthesize(String language, String text) throws Exception;

    /**
     * Speaks the text in the given language, handing over each part of the speech, such as a sentence, as soon as it
     * is ready. Called while the alarm plays; the runboard starts reading the first part when the alarm ends, while
     * the engine works on the rest, so that the speech starts sooner. The parts are played one after the other, so
     * they must be in the same format. The default calls {@link #synthesize(String, String)} and hands over its
     * result as a single part.
     *
     * @param parts receives the parts in the order they are spoken, on the thread that called this method
     * @return whether the engine can speak the language; if not, no parts are handed over
     * @throws Exception if synthesis fails; the runboard logs it and reads only the parts handed over until then
     */
    default boolean synthesize(String language, String text, Consumer<SpeechAudio> parts) throws Exception {
        final Optional<SpeechAudio> audio = synthesize(language, text);
        audio.ifPresent(parts);
        return audio.isPresent();
    }

    /**
     * Releases the engine's resources. Called once when the runboard exits, from a shutdown hook, after the speech
     * thread has stopped. Engines with native resources must free them here: native destructors that run after the
     * JVM has exited can crash it. The default does nothing.
     */
    @Override
    default void close() throws Exception {
    }
}
