package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import net.pkhapps.idispatch.runboard.speech.SpeechEngine;
import net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * The {@code fake} engine speaks the languages listed in its {@code languages} setting, producing one sample per
 * character. It hands over each sentence as a part of its own, and fails when it comes to the sentence "fail.". The
 * languages it has been asked to prepare are in {@link #PREPARED}.
 */
public class FakeSpeechEngineProvider implements SpeechEngineProvider {

    static final Set<String> PREPARED = ConcurrentHashMap.newKeySet();

    @Override
    public String name() {
        return "fake";
    }

    @Override
    public SpeechEngine create(SpeechSettings settings) {
        final List<String> languages = settings.get("languages").map(value -> List.of(value.split(","))).orElse(List.of());
        return new SpeechEngine() {
            @Override
            public boolean supportsLanguage(String language) {
                return languages.contains(language);
            }

            @Override
            public void prepare(String language) {
                PREPARED.add(language);
            }

            @Override
            public Optional<SpeechAudio> synthesize(String language, String text) {
                throw new UnsupportedOperationException("The runboard synthesizes in parts");
            }

            @Override
            public boolean synthesize(String language, String text, Consumer<SpeechAudio> parts) {
                if (!supportsLanguage(language)) {
                    return false;
                }
                for (String sentence : text.split("(?<=\\.) ")) {
                    if ("fail.".equals(sentence)) {
                        throw new IllegalStateException("Synthesis failed");
                    }
                    parts.accept(SpeechAudio.ofMono16Bit(new short[sentence.length()], 22050));
                }
                return true;
            }
        };
    }

    /**
     * The {@code failing} engine cannot be created.
     */
    public static class Failing implements SpeechEngineProvider {

        @Override
        public String name() {
            return "failing";
        }

        @Override
        public SpeechEngine create(SpeechSettings settings) throws Exception {
            throw new Exception("Native libraries missing");
        }
    }
}
