package net.pkhapps.idispatch.runboard;


import net.pkhapps.idispatch.runboard.client.Notification;
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
 * Sounds the alarm for new notifications and then reads them out loud, each one twice. Without speech, the alarm
 * plays for longer instead. Sound is played on a thread of its own, one alert at a time.
 */
class Alarm implements Observer {

    private static final int ALARM_PLAYS_BEFORE_SPEECH = 3;
    private static final int ALARM_PLAYS_WITHOUT_SPEECH = 9;
    private static final int SPEECH_REPEATS = 2;
    private static final long PAUSE_MILLISECONDS = 1000;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().name("audio").daemon().factory());
    private final Speech speech;
    private Clip alarmClip;
    private Clip beepClip;
    private Set<Notification> seenNotifications = new HashSet<>();

    Alarm(Speech speech) {
        this.speech = speech;
        try {
            alarmClip = openClip("/sounds/alarm.wav");
            beepClip = openClip("/sounds/beep.wav");
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            logger.error("Error initializing sound system, no sounds will be played", ex);
        }
        audioExecutor.execute(speech::initialize);
    }

    private static Clip openClip(String resource)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        final Clip clip = AudioSystem.getClip();
        clip.open(AudioSystem.getAudioInputStream(Alarm.class.getResource(resource)));
        return clip;
    }

    void soundBeep() {
        try {
            beepClip.stop();
            beepClip.loop(1);
        } catch (Exception ex) {
            logger.error("Error playing beep", ex);
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
            // Synthesize while the alarm plays, so that the speech is ready when it ends.
            final int alarmPlays = speech.isAvailable() ? ALARM_PLAYS_BEFORE_SPEECH : ALARM_PLAYS_WITHOUT_SPEECH;
            final AlarmPlayback alarm = startAlarm(alarmPlays);
            final List<Speech.Audio> announcements = notifications.stream()
                    .flatMap(n -> speech.synthesize(n.getLanguage(), Announcement.text(n)).stream())
                    .toList();
            alarm.await();
            if (announcements.isEmpty()) {
                if (alarmPlays < ALARM_PLAYS_WITHOUT_SPEECH) {
                    startAlarm(ALARM_PLAYS_WITHOUT_SPEECH - alarmPlays).await();
                }
                return;
            }
            for (Speech.Audio announcement : announcements) {
                for (int i = 0; i < SPEECH_REPEATS; i++) {
                    Thread.sleep(PAUSE_MILLISECONDS);
                    play(announcement);
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            logger.error("Error playing alert", ex);
        }
    }

    /**
     * Starts playing the alarm the given number of times and returns without waiting for it to finish.
     */
    private AlarmPlayback startAlarm(int plays) {
        final AlarmPlayback playback = new AlarmPlayback(plays);
        if (alarmClip != null) {
            alarmClip.stop();
            alarmClip.setFramePosition(0);
            alarmClip.addLineListener(playback);
            alarmClip.loop(plays - 1);
        } else {
            playback.stopped.countDown();
        }
        return playback;
    }

    private void play(Speech.Audio audio) throws LineUnavailableException {
        try (SourceDataLine line = AudioSystem.getSourceDataLine(audio.format())) {
            line.open(audio.format());
            line.start();
            line.write(audio.samples(), 0, audio.samples().length);
            line.drain();
        }
    }

    private class AlarmPlayback implements LineListener {

        private final CountDownLatch stopped = new CountDownLatch(1);
        private final long timeoutMilliseconds;

        private AlarmPlayback(int plays) {
            // Give up waiting a little after the alarm should have ended, in case the STOP event never arrives.
            timeoutMilliseconds = alarmClip == null ? 0 : alarmClip.getMicrosecondLength() * plays / 1000 + 2000;
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
                    logger.warn("Alarm did not stop in time, continuing");
                }
            } finally {
                if (alarmClip != null) {
                    alarmClip.removeLineListener(this);
                }
            }
        }
    }
}
