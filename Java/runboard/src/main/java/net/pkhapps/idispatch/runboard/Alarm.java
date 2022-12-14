package net.pkhapps.idispatch.runboard;


import net.pkhapps.idispatch.runboard.client.Notification;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class Alarm implements Observer {

    private AudioInputStream alarmWav;
    private AudioInputStream beepWav;
    private Clip alarmClip;
    private Clip beepClip;
    private Set<Notification> seenNotifications = new HashSet<>();

    Alarm() {
        try {
            alarmWav = AudioSystem.getAudioInputStream(Alarm.class
                    .getResource("/sounds/alarm.wav"));
            beepWav = AudioSystem.getAudioInputStream(Alarm.class.getResource("/sounds/beep.wav"));
            alarmClip = AudioSystem.getClip();
            alarmClip.open(alarmWav);
            beepClip = AudioSystem.getClip();
            beepClip.open(beepWav);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException ex) {
            Logger.getLogger(Alarm.class.getName()).log(Level.SEVERE, "Error initializing sound system, no sounds will be played", ex);
        }
    }

    void soundAlarm() {
        try {
            alarmClip.stop();
            alarmClip.loop(8);
        } catch (Exception ex) {
            Logger.getLogger(Alarm.class.getName()).log(Level.SEVERE, "Error playing alarm", ex);
        }
    }

    void soundBeep() {
        try {
            beepClip.stop();
            beepClip.loop(1);
        } catch (Exception ex) {
            Logger.getLogger(Alarm.class.getName()).log(Level.SEVERE, "Error playing beep", ex);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        Model model = (Model) o;
        Set<Notification> notificationsToDelete = new HashSet<>(seenNotifications);
        for (Notification n : model.getVisibleNotifications()) {
            if (!notificationsToDelete.remove(n)) {
                seenNotifications.add(n);
                soundAlarm();
            }
        }
        seenNotifications.removeAll(notificationsToDelete);
        if (model.hasError()) {
            soundBeep();
        }
    }
}
