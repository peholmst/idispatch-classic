package net.pkhapps.idispatch.runboard;


import net.pkhapps.idispatch.runboard.client.Notification;
import net.pkhapps.idispatch.runboard.speech.SpeechAudio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Sounds the alarm for new notifications and then reads them out loud. Without speech, the alarm plays for longer
 * instead. {@link AlertTimings} sets how many times each part plays. Sound is played on a thread of its own, one alert
 * or beep at a time, with the amplifier switched on. The alarm and the beep are attenuated by the alarm volume, so that
 * they are not much louder than the speech.
 */
class Alarm implements Observer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("audio").daemon().factory());
    private final Speech speech;
    private final Outputs outputs;
    private final AlertTimings timings;
    private Clip alarmClip;
    private Clip beepClip;
    private Set<Notification> seenNotifications = new HashSet<>();

    /**
     * @param alarmVolume the gain of the alarm and the beep in decibels, at most 0
     */
    Alarm(Speech speech, Outputs outputs, AlertTimings timings, double alarmVolume) {
        this.speech = speech;
        this.outputs = outputs;
        this.timings = timings;
        try {
            alarmClip = openClip("/sounds/alarm.wav", alarmVolume);
            beepClip = openClip("/sounds/beep.wav", alarmVolume);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            logger.error("Error initializing sound system, no sounds will be played", ex);
        }
        audioExecutor.execute(speech::initialize);
    }

    /**
     * Stops the audio thread, cutting off any alert that is playing, and then releases the speech engine. Called when
     * the runboard exits.
     */
    void close() {
        audioExecutor.shutdownNow();
        try {
            if (audioExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                speech.close();
            } else {
                logger.warn("Audio thread did not stop, leaving the speech engine open");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static Clip openClip(String resource, double volume)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        try (AudioInputStream file = AudioSystem.getAudioInputStream(Alarm.class.getResource(resource))) {
            final AudioFormat format = new AudioFormat(file.getFormat().getSampleRate(), 16,
                    file.getFormat().getChannels(), true, false);
            try (AudioInputStream pcm = AudioSystem.getAudioInputStream(format, file)) {
                final byte[] samples = Volume.amplify(pcm.readAllBytes(), format, volume);
                final Clip clip = AudioSystem.getClip();
                clip.open(format, samples, 0, samples.length);
                return clip;
            }
        }
    }

    void soundBeep() {
        audioExecutor.execute(this::beep);
    }

    private void beep() {
        if (beepClip == null) {
            return;
        }
        try {
            outputs.switchOnAmplifier();
            startClip(beepClip, 2).await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            logger.error("Error playing beep", ex);
        } finally {
            outputs.switchOffAmplifier();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        Model model = (Model) o;
        Set<Notification> notificationsToDelete = new HashSet<>(seenNotifications);
        List<Notification> newNotifications = new ArrayList<>();
        for (Notification n : model.getVisibleNotifications()) {
            if (!notificationsToDelete.remove(n)) {
                seenNotifications.add(n);
                newNotifications.add(n);
            }
        }
        seenNotifications.removeAll(notificationsToDelete);
        if (!newNotifications.isEmpty()) {
            audioExecutor.execute(() -> alert(newNotifications));
        }
        if (model.hasError()) {
            soundBeep();
        }
    }

    private void alert(List<Notification> notifications) {
        try {
            outputs.switchOnAmplifier();
            // Synthesize while the alarm plays, so that the speech is ready when it ends.
            final boolean speechExpected = notifications.stream()
                    .anyMatch(n -> speech.supportsLanguage(speech.languageOf(n)));
            final int alarmPlays = speechExpected ? timings.alarmPlaysBeforeSpeech() : timings.alarmPlays();
            final ClipPlayback alarm = startAlarm(alarmPlays);
            final List<SpeechAudio> announcements = notifications.stream()
                    .flatMap(n -> {
                        final String language = speech.languageOf(n);
                        logger.info("Notification {} came in language '{}' from the server, reading it in '{}'",
                                n.getId(), n.getLanguage().isEmpty() ? "none" : n.getLanguage(),
                                language.isEmpty() ? "none" : language);
                        return speech.synthesize(language, Announcement.text(n, language)).stream();
                    })
                    .toList();
            alarm.await();
            if (announcements.isEmpty()) {
                if (alarmPlays < timings.alarmPlays()) {
                    startAlarm(timings.alarmPlays() - alarmPlays).await();
                }
                return;
            }
            for (SpeechAudio announcement : announcements) {
                for (int i = 0; i < timings.speechRepeats(); i++) {
                    Thread.sleep(timings.speechPause());
                    play(announcement);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            logger.error("Error playing alert", ex);
        } finally {
            outputs.switchOffAmplifier();
        }
    }

    /**
     * Starts playing the alarm the given number of times and returns without waiting for it to finish.
     */
    private ClipPlayback startAlarm(int plays) {
        return startClip(alarmClip, plays);
    }

    /**
     * Starts playing the clip from the start the given number of times and returns without waiting for it to finish.
     */
    private ClipPlayback startClip(Clip clip, int plays) {
        final ClipPlayback playback = new ClipPlayback(clip, plays);
        if (clip != null) {
            clip.stop();
            clip.setFramePosition(0);
            clip.addLineListener(playback);
            clip.loop(plays - 1);
        } else {
            playback.stopped.countDown();
        }
        return playback;
    }

    private void play(SpeechAudio audio) throws LineUnavailableException {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(audio.format())) {
            line.open(audio.format());
            line.start();
            line.write(audio.samples(), 0, audio.samples().length);
            line.drain();
        }
    }

    private class ClipPlayback implements LineListener {

        private final CountDownLatch stopped = new CountDownLatch(1);
        private final Clip clip;
        private final long timeoutMilliseconds;

        private ClipPlayback(Clip clip, int plays) {
            this.clip = clip;
            // Give up waiting a little after the clip should have ended, in case the STOP event never arrives.
            timeoutMilliseconds = clip == null ? 0 : clip.getMicrosecondLength() * plays / 1000 + 2000;
        }

        @Override
        public void update(LineEvent event) {
            if (event.getType() == LineEvent.Type.STOP) {
                stopped.countDown();
            }
        }

        void await() throws InterruptedException {
            try {
                if (!stopped.await(timeoutMilliseconds, TimeUnit.MILLISECONDS)) {
                    logger.warn("Sound did not stop in time, continuing");
                }
            } finally {
                if (clip != null) {
                    clip.removeLineListener(this);
                }
            }
        }
    }
}
