/**
 * Keeps the screen on while the app is visible, with the Screen Wake Lock API. The browser lets the lock go whenever the
 * app is hidden, so it is taken again each time the app becomes visible.
 */
export interface ScreenLock {
  /** @return whether the screen is kept on; not every browser can */
  acquire(): Promise<boolean>;

  release(): Promise<void>;
}

export class WakeLockScreenLock implements ScreenLock {

  private sentinel: WakeLockSentinel | null = null;

  async acquire() {
    if (!('wakeLock' in navigator)) {
      return false;
    }
    if (this.sentinel !== null && !this.sentinel.released) {
      return true;
    }
    try {
      this.sentinel = await navigator.wakeLock.request('screen');
      return true;
    } catch {
      // Refused, as when the app is not visible or the battery is low
      return false;
    }
  }

  async release() {
    await this.sentinel?.release();
    this.sentinel = null;
  }
}
