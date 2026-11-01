package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SpeechTest {

    private static final SpeechSettings FINNISH_ONLY = settings(Map.of("languages", "fi"));
    private static final Set<String> LANGUAGES = Set.of("fi", "sv");

    private final List<Speech> started = new ArrayList<>();

    @BeforeEach
    void forgetPreparedLanguages() {
        FakeSpeechEngineProvider.PREPARED.clear();
    }

    @AfterEach
    void close() {
        started.forEach(Speech::close);
    }

    @Test
    void configuredEngine_isFoundAndGivenItsSettings() throws Exception {
        var speech = started("fake", FINNISH_ONLY);

        assertTrue(speech.supportsLanguage("fi"));
        assertFalse(speech.supportsLanguage("sv"));
        // One 16-bit sample per character, two bytes each
        assertEquals(List.of(20), partLengths(speech.read("fi", "Tehtävä 1.")));
        assertEquals(List.of(), partLengths(speech.read("sv", "Uppdrag 1.")));
    }

    @Test
    void speech_isHandedOverASentenceAtATime() throws Exception {
        var speech = started("fake", FINNISH_ONLY);

        assertEquals(List.of(58, 20, 22), partLengths(speech.read("fi", "Hälytysilmoitus: R V S 2 0 1. Tehtävä 1. Kotikatu 1.")));
    }

    @Test
    void textsAreRead_inTheOrderTheyWereGiven() throws Exception {
        var speech = started("fake", FINNISH_ONLY);

        var first = speech.read("fi", "Tehtävä 1.");
        var second = speech.read("fi", "Tehtävä 2 0.");

        assertEquals(List.of(24), partLengths(second));
        assertEquals(List.of(20), partLengths(first));
    }

    @Test
    void supportedLanguages_arePreparedWhenStarted() throws Exception {
        var speech = started("fake", FINNISH_ONLY);

        // Reading waits for the speech thread, which prepares the languages first.
        partLengths(speech.read("fi", "Tehtävä 1."));

        assertEquals(Set.of("fi"), FakeSpeechEngineProvider.PREPARED);
    }

    @Test
    void unknownEngine_speaksNothing() throws Exception {
        var speech = started("missing", FINNISH_ONLY);

        assertFalse(speech.supportsLanguage("fi"));
        assertEquals(List.of(), partLengths(speech.read("fi", "Tehtävä 1.")));
    }

    @Test
    void engineNone_speaksNothing() throws Exception {
        var speech = started(Speech.NONE, FINNISH_ONLY);

        assertFalse(speech.supportsLanguage("fi"));
        assertEquals(List.of(), partLengths(speech.read("fi", "Tehtävä 1.")));
    }

    @Test
    void engineThatCannotBeCreated_speaksNothing() throws Exception {
        var speech = started("failing", FINNISH_ONLY);

        assertFalse(speech.supportsLanguage("fi"));
        assertEquals(List.of(), partLengths(speech.read("fi", "Tehtävä 1.")));
    }

    @Test
    void failedSynthesis_endsAfterThePartsSynthesizedBeforeIt() throws Exception {
        var speech = started("fake", FINNISH_ONLY);

        assertEquals(List.of(20), partLengths(speech.read("fi", "Tehtävä 1. fail. Kotikatu 1.")));
        assertEquals(List.of(20), partLengths(speech.read("fi", "Tehtävä 1.")), "the next text is read as usual");
    }

    @Test
    void closedSpeech_speaksNothing() throws Exception {
        var speech = started("fake", FINNISH_ONLY);

        speech.close();
        speech.close();

        assertFalse(speech.supportsLanguage("fi"));
        assertEquals(List.of(), partLengths(speech.read("fi", "Tehtävä 1.")));
    }

    @Test
    void speechClosedBeforeItStarted_speaksNothing() throws Exception {
        var speech = new Speech("fake", FINNISH_ONLY, "", LANGUAGES);

        speech.close();

        assertFalse(speech.supportsLanguage("fi"));
        assertEquals(List.of(), partLengths(speech.read("fi", "Tehtävä 1.")));
    }

    @Test
    void notificationWithoutLanguage_isReadInTheDefaultLanguage() {
        var speech = new Speech("fake", FINNISH_ONLY, "fi", LANGUAGES);

        assertEquals("fi", speech.languageOf(new Notification(Map.of())));
        assertEquals("sv", speech.languageOf(new Notification(Map.of("language", "sv"))));
    }

    @Test
    void withoutDefaultLanguage_notificationWithoutLanguageHasNone() {
        var speech = new Speech("fake", FINNISH_ONLY, "", LANGUAGES);

        assertEquals("", speech.languageOf(new Notification(Map.of())));
    }

    private Speech started(String engineName, SpeechSettings settings) {
        var speech = new Speech(engineName, settings, "", LANGUAGES);
        speech.start();
        started.add(speech);
        return speech;
    }

    /**
     * Waits for every part of the utterance and returns their lengths in bytes.
     */
    private static List<Integer> partLengths(Utterance utterance) throws InterruptedException {
        final List<Integer> lengths = new ArrayList<>();
        for (Optional<SpeechAudio> part = utterance.next(); part.isPresent(); part = utterance.next()) {
            lengths.add(part.get().samples().length);
        }
        return lengths;
    }

    private static SpeechSettings settings(Map<String, String> values) {
        return name -> Optional.ofNullable(values.get(name));
    }
}
