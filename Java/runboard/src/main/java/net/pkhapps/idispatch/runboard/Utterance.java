package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechAudio;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Speech that is being synthesized on the speech thread, handed over to the audio thread a part at a time as each part
 * is ready, so that reading it can start before all of it has been synthesized.
 */
final class Utterance {

    private final BlockingQueue<Optional<SpeechAudio>> parts = new LinkedBlockingQueue<>();
    private final Duration timeout;
    private boolean ended;

    /**
     * @param timeout how long to wait for each part before giving up on the rest
     */
    Utterance(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * Adds the next part. Called from the speech thread.
     */
    void add(SpeechAudio part) {
        parts.add(Optional.of(part));
    }

    /**
     * Marks the end of the speech: there are no more parts. Called from the speech thread, also when synthesis fails.
     */
    void end() {
        parts.add(Optional.empty());
    }

    /**
     * Waits for the next part. Called from the audio thread.
     *
     * @return the next part, or an empty optional if there are no more, or if the next one did not come within the
     * timeout; after that, always an empty optional
     */
    Optional<SpeechAudio> next() throws InterruptedException {
        if (ended) {
            return Optional.empty();
        }
        final Optional<SpeechAudio> part = parts.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (part == null || part.isEmpty()) {
            ended = true;
            return Optional.empty();
        }
        return part;
    }
}
