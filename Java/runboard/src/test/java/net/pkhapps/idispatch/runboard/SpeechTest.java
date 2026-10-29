package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SpeechTest {

    private static final SpeechSettings FINNISH_ONLY = settings(Map.of("languages", "fi"));

    @Test
    void configuredEngine_isFoundAndGivenItsSettings() {
        var speech = initialized("fake", FINNISH_ONLY);

        assertTrue(speech.supportsLanguage("fi"));
        assertFalse(speech.supportsLanguage("sv"));
        // One 16-bit sample per character, two bytes each
        assertEquals(20, speech.synthesize("fi", "Tehtävä 1.").orElseThrow().samples().length);
        assertTrue(speech.synthesize("sv", "Uppdrag 1.").isEmpty());
    }

    @Test
    void unknownEngine_speaksNothing() {
        var speech = initialized("missing", FINNISH_ONLY);

        assertFalse(speech.supportsLanguage("fi"));
        assertTrue(speech.synthesize("fi", "Tehtävä 1.").isEmpty());
    }

    @Test
    void engineNone_speaksNothing() {
        var speech = initialized(Speech.NONE, FINNISH_ONLY);

        assertFalse(speech.supportsLanguage("fi"));
        assertTrue(speech.synthesize("fi", "Tehtävä 1.").isEmpty());
    }

    @Test
    void engineThatCannotBeCreated_speaksNothing() {
        var speech = initialized("failing", FINNISH_ONLY);

        assertFalse(speech.supportsLanguage("fi"));
        assertTrue(speech.synthesize("fi", "Tehtävä 1.").isEmpty());
    }

    @Test
    void failedSynthesis_isReturnedAsNoSpeech() {
        var speech = initialized("fake", FINNISH_ONLY);

        assertTrue(speech.synthesize("fi", "fail").isEmpty());
    }

    @Test
    void closedSpeech_speaksNothing() {
        var speech = initialized("fake", FINNISH_ONLY);

        speech.close();
        speech.close();

        assertFalse(speech.supportsLanguage("fi"));
        assertTrue(speech.synthesize("fi", "Tehtävä 1.").isEmpty());
    }

    @Test
    void notificationWithoutLanguage_isReadInTheDefaultLanguage() {
        var speech = new Speech("fake", FINNISH_ONLY, "fi");

        assertEquals("fi", speech.languageOf(new Notification(Map.of())));
        assertEquals("sv", speech.languageOf(new Notification(Map.of("language", "sv"))));
    }

    @Test
    void withoutDefaultLanguage_notificationWithoutLanguageHasNone() {
        var speech = new Speech("fake", FINNISH_ONLY, "");

        assertEquals("", speech.languageOf(new Notification(Map.of())));
    }

    private static Speech initialized(String engineName, SpeechSettings settings) {
        var speech = new Speech(engineName, settings, "");
        speech.initialize();
        return speech;
    }

    private static SpeechSettings settings(Map<String, String> values) {
        return name -> Optional.ofNullable(values.get(name));
    }
}
