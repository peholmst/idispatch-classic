package net.pkhapps.idispatch.runboard;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperVoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Turns text into speech with the Piper voice configured for its language. Voices are loaded the first time they are
 * needed. Not thread safe: use one instance from a single thread.
 */
class Speech {

    /**
     * Speech as signed 16-bit little-endian PCM.
     */
    record Audio(byte[] samples, AudioFormat format) {
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Configuration configuration;
    private final Map<String, Optional<PiperVoice>> voices = new HashMap<>();
    private PiperJNI piper;

    Speech(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Loads the native Piper libraries. If this fails, {@link #isAvailable()} returns false and nothing is spoken.
     */
    void initialize() {
        try {
            final PiperJNI piper = new PiperJNI();
            piper.initialize();
            this.piper = piper;
            logger.info("Initialized Piper {}", piper.getPiperVersion());
        } catch (Exception | LinkageError ex) {
            logger.error("Could not initialize Piper, notifications will not be read out loud", ex);
        }
    }

    boolean isAvailable() {
        return piper != null;
    }

    /**
     * Returns the text spoken in the given language, or an empty optional if there is no voice for the language or
     * synthesis fails.
     */
    Optional<Audio> synthesize(String language, String text) {
        return voice(language).flatMap(voice -> {
            try {
                final long start = System.nanoTime();
                final short[] samples = piper.textToAudio(voice, text);
                logger.debug("Synthesized {} samples in {} ms", samples.length, (System.nanoTime() - start) / 1_000_000);
                final ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);
                buffer.asShortBuffer().put(samples);
                return Optional.of(new Audio(buffer.array(), new AudioFormat(voice.getSampleRate(), 16, 1, true, false)));
            } catch (Exception ex) {
                logger.error("Could not synthesize speech for text: {}", text, ex);
                return Optional.empty();
            }
        });
    }

    private Optional<PiperVoice> voice(String language) {
        if (piper == null) {
            return Optional.empty();
        }
        return voices.computeIfAbsent(language, this::loadVoice);
    }

    private Optional<PiperVoice> loadVoice(String language) {
        final Optional<Path> model = configuration.getVoiceModel(language);
        if (model.isEmpty()) {
            logger.warn("No voice configured for language '{}', notifications in it will not be read out loud", language);
            return Optional.empty();
        }
        final Path config = model.get().resolveSibling(model.get().getFileName() + ".json");
        try {
            final long start = System.nanoTime();
            final PiperVoice voice = piper.loadVoice(model.get(), config);
            logger.info("Loaded voice {} for language '{}' in {} ms", model.get(), language,
                    (System.nanoTime() - start) / 1_000_000);
            return Optional.of(voice);
        } catch (Exception ex) {
            logger.error("Could not load voice {} for language '{}'", model.get(), language, ex);
            return Optional.empty();
        }
    }
}
