package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import net.pkhapps.idispatch.runboard.speech.SpeechEngine;
import net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Reads text out loud with the {@link SpeechEngine} selected by {@code idispatch.speech.engine}, found with
 * {@link ServiceLoader}. Errors from the engine are logged and never reach the caller, so that a broken engine only
 * costs the speech, not the alarm. Use from a single thread, starting with {@link #initialize()}.
 */
class Speech {

    /**
     * The engine name that turns speech off.
     */
    static final String NONE = "none";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String engineName;
    private final SpeechSettings settings;
    private SpeechEngine engine;

    Speech(Configuration configuration) {
        this(configuration.getSpeechEngine(), configuration.getSpeechSettings());
    }

    Speech(String engineName, SpeechSettings settings) {
        this.engineName = engineName;
        this.settings = settings;
    }

    /**
     * Creates the engine. If it cannot be found or created, nothing is spoken.
     */
    void initialize() {
        if (NONE.equals(engineName)) {
            logger.info("Speech is turned off, notifications will not be read out loud");
            return;
        }
        try {
            final List<SpeechEngineProvider> providers = ServiceLoader.load(SpeechEngineProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .toList();
            final Optional<SpeechEngineProvider> provider = providers.stream()
                    .filter(p -> p.name().equals(engineName))
                    .findFirst();
            if (provider.isEmpty()) {
                logger.error("No speech engine named '{}', notifications will not be read out loud. Available: {}",
                        engineName, providers.stream().map(SpeechEngineProvider::name).toList());
                return;
            }
            engine = provider.get().create(settings);
            logger.info("Using speech engine '{}'", engineName);
        } catch (Exception | ServiceConfigurationError | LinkageError ex) {
            logger.error("Could not create speech engine '{}', notifications will not be read out loud", engineName, ex);
        }
    }

    /**
     * Returns whether text in the given language can be read out loud.
     */
    boolean supportsLanguage(String language) {
        if (engine == null) {
            return false;
        }
        try {
            return engine.supportsLanguage(language);
        } catch (RuntimeException ex) {
            logger.error("Speech engine '{}' failed to check language '{}'", engineName, language, ex);
            return false;
        }
    }

    /**
     * Returns the text spoken in the given language, or an empty optional if it cannot be spoken.
     */
    Optional<SpeechAudio> synthesize(String language, String text) {
        if (engine == null) {
            return Optional.empty();
        }
        try {
            return engine.synthesize(language, text);
        } catch (Exception | LinkageError ex) {
            logger.error("Speech engine '{}' could not synthesize text in language '{}': {}", engineName, language,
                    text, ex);
            return Optional.empty();
        }
    }
}
