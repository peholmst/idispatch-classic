package net.pkhapps.idispatch.runboard.speech.piper;

import io.github.jvoiceproject.piperjni.PiperJNI;
import io.github.jvoiceproject.piperjni.PiperVoice;
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
import java.util.function.Consumer;

/**
 * Speaks with Piper voices. The native libraries are loaded when the engine is created, and each language's voice when
 * it is {@linkplain #prepare(String) prepared} or first needed. Piper synthesizes a sentence at a time, and each
 * sentence is handed over as a part of its own.
 */
class PiperSpeechEngine implements SpeechEngine {

    /**
     * Synthesized once for each voice when it is prepared. The first synthesis with a voice is slower than the ones
     * after it, as onnxruntime sets itself up, so this makes the first notification as fast as the rest.
     */
    private static final String WARM_UP_TEXT = "1 2 3.";

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
    public void prepare(String language) throws Exception {
        final Optional<PiperVoice> voice = voice(language);
        if (voice.isPresent()) {
            final long start = System.nanoTime();
            piper.textToAudio(voice.get(), WARM_UP_TEXT);
            logger.info("Warmed up the voice for language '{}' in {} ms", language,
                    (System.nanoTime() - start) / 1_000_000);
        }
    }

    @Override
    public Optional<SpeechAudio> synthesize(String language, String text) throws Exception {
        final Optional<PiperVoice> voice = voice(language);
        if (voice.isEmpty()) {
            return Optional.empty();
        }
        final short[] samples = piper.textToAudio(voice.get(), text);
        return Optional.of(SpeechAudio.ofMono16Bit(samples, voice.get().getSampleRate()));
    }

    @Override
    public boolean synthesize(String language, String text, Consumer<SpeechAudio> parts) throws Exception {
        final Optional<PiperVoice> voice = voice(language);
        if (voice.isEmpty()) {
            return false;
        }
        final int sampleRate = voice.get().getSampleRate();
        piper.textToAudio(voice.get(), text, samples -> parts.accept(SpeechAudio.ofMono16Bit(samples, sampleRate)));
        return true;
    }

    private Optional<PiperVoice> voice(String language) {
        final Optional<PiperVoice> voice = voices.computeIfAbsent(language, this::loadVoice);
        if (voice.isEmpty()) {
            // Try again next time, in case the voice has been downloaded since.
            voices.remove(language);
        }
        return voice;
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
