package net.pkhapps.idispatch.runboard.speech.piper;

import io.github.givimad.piperjni.PiperJNI;
import io.github.givimad.piperjni.PiperVoice;
import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import net.pkhapps.idispatch.runboard.speech.SpeechEngine;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Speaks with Piper voices. The native libraries are loaded when the engine is created, and each language's voice
 * the first time it is needed.
 */
class PiperSpeechEngine implements SpeechEngine {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SpeechSettings settings;
    private final PiperJNI piper;
    private final Map<String, Optional<PiperVoice>> voices = new HashMap<>();

    PiperSpeechEngine(SpeechSettings settings) throws IOException {
        this.settings = settings;
        piper = new PiperJNI();
        piper.initialize();
        logger.info("Initialized Piper {}", piper.getPiperVersion());
    }

    @Override
    public boolean supportsLanguage(String language) {
        return voiceModel(language).isPresent();
    }

    @Override
    public Optional<SpeechAudio> synthesize(String language, String text) throws Exception {
        final Optional<PiperVoice> voice = voices.computeIfAbsent(language, this::loadVoice);
        if (voice.isEmpty()) {
            return Optional.empty();
        }
        final long start = System.nanoTime();
        final short[] samples = piper.textToAudio(voice.get(), text);
        logger.debug("Synthesized {} samples in {} ms", samples.length, (System.nanoTime() - start) / 1_000_000);
        return Optional.of(SpeechAudio.ofMono16Bit(samples, voice.get().getSampleRate()));
    }

    private Optional<Path> voiceModel(String language) {
        return settings.get("voice." + language).map(Path::of);
    }

    private Optional<PiperVoice> loadVoice(String language) {
        final Optional<Path> model = voiceModel(language);
        if (model.isEmpty()) {
            logger.warn("No Piper voice configured for language '{}'", language);
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

    @Override
    public void close() {
        voices.values().forEach(voice -> voice.ifPresent(PiperVoice::close));
        voices.clear();
        piper.close();
    }
}
