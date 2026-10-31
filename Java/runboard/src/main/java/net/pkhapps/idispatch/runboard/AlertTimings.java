package net.pkhapps.idispatch.runboard;

import java.time.Duration;

/**
 * How an alert for new notifications plays out.
 *
 * @param alarmPlays             how many times the alarm plays when the notifications are not read out loud
 * @param alarmPlaysBeforeSpeech how many times the alarm plays before the notifications are read out loud, at most
 *                               {@code alarmPlays}
 * @param speechRepeats          how many times each notification is read out loud
 * @param speechPause            the silence before each reading
 */
record AlertTimings(int alarmPlays, int alarmPlaysBeforeSpeech, int speechRepeats, Duration speechPause) {
}
