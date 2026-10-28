package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import net.pkhapps.idispatch.runboard.speech.SpeechEngine;
import net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;

import java.util.List;
import java.util.Optional;

/**
 * The {@code fake} engine speaks the languages listed in its {@code languages} setting, producing one sample per
 * character, and fails to synthesize the text "fail".
 */
public class FakeSpeechEngineProvider implements SpeechEngineProvider {

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
            public Optional<SpeechAudio> synthesize(String language, String text) {
                if ("fail".equals(text)) {
                    throw new IllegalStateException("Synthesis failed");
                }
                return supportsLanguage(language)
                        ? Optional.of(SpeechAudio.ofMono16Bit(new short[text.length()], 22050))
                        : Optional.empty();
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
