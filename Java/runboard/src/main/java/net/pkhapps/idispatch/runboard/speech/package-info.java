/**
 * Service provider interface for the speech engines that read notifications out loud.
 * <p>
 * An engine is plugged in by implementing {@link net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider} and
 * listing the implementation in {@code META-INF/services/net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider},
 * so that {@link java.util.ServiceLoader} finds it. The runboard uses the provider whose
 * {@linkplain net.pkhapps.idispatch.runboard.speech.SpeechEngineProvider#name() name} is set by
 * {@code idispatch.speech.engine}. See {@code runboard/docs/speech.md} for a walkthrough.
 */
package net.pkhapps.idispatch.runboard.speech;
