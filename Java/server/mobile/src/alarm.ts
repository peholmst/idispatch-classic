/**
 * The alarm a new alert sounds while the unit is on call: two alternating tones, made with Web Audio so that there is
 * no sound file to ship. Browsers play sound only after the user has touched the page, so {@link unlock} must be called
 * from a tap, such as the one that starts the on-call duty.
 * <p>
 * Web Audio plays at full volume within the page; the device's own volume still applies, which the app can't change.
 */
export interface Alarm {
  /** Lets the alarm play later; call it from a tap. */
  unlock(): Promise<void>;

  /** Sounds the alarm until {@link stop} is called, or for {@link MAX_ALARM_MS} at most. */
  start(): void;

  stop(): void;

  readonly sounding: boolean;
}

/** How long the alarm sounds if nobody acknowledges the alert. */
export const MAX_ALARM_MS = 60_000;

const TONES_HZ = [960, 770];
const TONE_S = 0.45;

export class WebAudioAlarm implements Alarm {

  private context: AudioContext | null = null;
  private output: GainNode | null = null;
  private timer: ReturnType<typeof setTimeout> | null = null;
  private stopAt = 0;

  sounding = false;

  async unlock() {
    if (this.context === null) {
      this.context = new AudioContext();
      this.output = this.context.createGain();
      this.output.gain.value = 1;
      this.output.connect(this.context.destination);
    }
    await this.context.resume();
  }

  start() {
    if (this.context === null || this.sounding) {
      return;
    }
    this.sounding = true;
    this.stopAt = Date.now() + MAX_ALARM_MS;
    this.playCycle();
  }

  stop() {
    this.sounding = false;
    if (this.timer !== null) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  /** Plays one high and one low tone, then the next cycle, until stopped. */
  private playCycle() {
    if (!this.sounding || this.context === null || this.output === null) {
      return;
    }
    if (Date.now() >= this.stopAt) {
      this.stop();
      return;
    }
    const start = this.context.currentTime;
    TONES_HZ.forEach((frequency, index) => {
      const oscillator = this.context!.createOscillator();
      const envelope = this.context!.createGain();
      oscillator.type = 'square';
      oscillator.frequency.value = frequency;
      const at = start + index * TONE_S;
      // Short ramps, so that the tones don't click
      envelope.gain.setValueAtTime(0, at);
      envelope.gain.linearRampToValueAtTime(0.6, at + 0.02);
      envelope.gain.setValueAtTime(0.6, at + TONE_S - 0.02);
      envelope.gain.linearRampToValueAtTime(0, at + TONE_S);
      oscillator.connect(envelope).connect(this.output!);
      oscillator.start(at);
      oscillator.stop(at + TONE_S);
    });
    this.timer = setTimeout(() => this.playCycle(), TONES_HZ.length * TONE_S * 1000);
  }
}
