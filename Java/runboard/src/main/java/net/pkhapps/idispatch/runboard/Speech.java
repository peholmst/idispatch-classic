package net.pkhapps.idispatch.runboard;

import net.pkhapps.idispatch.runboard.client.Notification;
import net.pkhapps.idispatch.runboard.speech.SpeechEngine;
import net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider;
import net.pkhapps.idispatch.runboard.speech.SpeechSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Reads text out loud with the {@link SpeechEngine} selected by {@code idispatch.speech.engine}, found with
 * {@link ServiceLoader}. The engine is created, prepared and used on a thread of its own, the speech thread, so that
 * the audio thread can play the first part of the speech while the rest is synthesized. Errors from the engine are
 * logged and never reach the caller, so that a broken engine only costs the speech, not the alarm.
 */
class Speech {

    /**
     * The engine name that turns speech off.
     */
    static final String NONE = "none";

    /**
     * How long to wait for each part of the speech before giving up on the rest, in case the engine hangs.
     */
    private static final Duration PART_TIMEOUT = Duration.ofSeconds(30);

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService speechExecutor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("speech").daemon().factory());
    private final CountDownLatch created = new CountDownLatch(1);
    private final String engineName;
    private final SpeechSettings settings;
    private final String defaultLanguage;
    private final Collection<String> languages;
    private volatile SpeechEngine engine;

    Speech(Configuration configuration) {
        this(configuration.getSpeechEngine(), configuration.getSpeechSettings(),
                configuration.getSpeechDefaultLanguage(), Announcement.languages());
    }

    /**
     * @param languages the languages that notifications can come in, prepared when the engine is created
     */
    Speech(String engineName, SpeechSettings settings, String defaultLanguage, Collection<String> languages) {
        this.engineName = engineName;
        this.settings = settings;
        this.defaultLanguage = defaultLanguage;
        this.languages = List.copyOf(languages);
    }

    /**
     * Returns the language to read the notification in: its own, or the default language if the server did not say,
     * as servers from before the language setting don't.
     */
    String languageOf(Notification notification) {
        return notification.getLanguage().isBlank() ? defaultLanguage : notification.getLanguage();
    }

    /**
     * Creates the engine on the speech thread, and then prepares the languages it supports, so that the first
     * notification is read as fast as the rest. Returns without waiting. If the engine cannot be found or created,
     * nothing is spoken.
     */
    void start() {
        try {
            speechExecutor.execute(() -> {
                create();
                prepare();
            });
        } catch (RejectedExecutionException ex) {
            created.countDown();
        }
    }

    private void create() {
        try {
            logger.info("Notifications that come without a language from the server are read in '{}' "
                    + "(idispatch.speech.defaultLanguage)", defaultLanguage.isEmpty() ? "none" : defaultLanguage);
            if (NONE.equals(engineName)) {
                logger.info("Speech is turned off, notifications will not be read out loud");
                return;
            }
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
        } finally {
            created.countDown();
        }
    }

    private void prepare() {
        for (String language : languages) {
            if (supportsLanguage(language)) {
                try {
                    engine.prepare(language);
                } catch (Exception | LinkageError ex) {
                    logger.error("Speech engine '{}' could not prepare language '{}'", engineName, language, ex);
                }
            }
        }
    }

    /**
     * Stops the speech thread and releases the engine. Nothing is spoken after this.
     */
    void close() {
        speechExecutor.shutdownNow();
        created.countDown();
        try {
            if (!speechExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Speech thread did not stop, leaving the speech engine open");
                return;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        }
        final SpeechEngine closing = engine;
        engine = null;
        if (closing == null) {
            return;
        }
        try {
            closing.close();
            logger.info("Closed speech engine '{}'", engineName);
        } catch (Exception | LinkageError ex) {
            logger.error("Could not close speech engine '{}'", engineName, ex);
        }
    }

    /**
     * Returns whether text in the given language can be read out loud. While the engine is being created, when the
     * runboard starts, this waits for it.
     */
    boolean supportsLanguage(String language) {
        try {
            created.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
        final SpeechEngine current = engine;
        if (current == null) {
            return false;
        }
        try {
            return current.supportsLanguage(language);
        } catch (RuntimeException ex) {
            logger.error("Speech engine '{}' failed to check language '{}'", engineName, language, ex);
            return false;
        }
    }

    /**
     * Starts reading the text in the given language on the speech thread, after any text started before it, and
     * returns without waiting. Each part of the speech is {@linkplain Volume#normalize normalized}, so every voice is
     * as loud. If the text cannot be spoken, the utterance ends without parts; if synthesis fails, it ends after the
     * parts synthesized until then.
     */
    Utterance read(String language, String text) {
        final Utterance utterance = new Utterance(PART_TIMEOUT);
        try {
            speechExecutor.execute(() -> synthesize(language, text, utterance));
        } catch (RejectedExecutionException ex) {
            utterance.end();
        }
        return utterance;
    }

    private void synthesize(String language, String text, Utterance utterance) {
        try {
            final SpeechEngine current = engine;
            if (current == null) {
                return;
            }
            final long start = System.nanoTime();
            final long[] firstPartAt = {0};
            final int[] partCount = {0};
            final boolean supported = current.synthesize(language, text, part -> {
                if (partCount[0]++ == 0) {
                    firstPartAt[0] = System.nanoTime();
                }
                utterance.add(Volume.normalize(part));
            });
            if (supported) {
                logger.info("Synthesized {} parts in {} ms, the first in {} ms", partCount[0],
                        (System.nanoTime() - start) / 1_000_000,
                        partCount[0] == 0 ? 0 : (firstPartAt[0] - start) / 1_000_000);
            }
        } catch (Exception | LinkageError ex) {
            logger.error("Speech engine '{}' could not synthesize text in language '{}': {}", engineName, language,
                    text, ex);
        } finally {
            utterance.end();
        }
    }
}
