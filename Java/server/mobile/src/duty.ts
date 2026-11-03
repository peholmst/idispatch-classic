import type { Alarm } from './alarm';
import type { ScreenLock } from './wake-lock';
import { run, SETTINGS } from './db';

/** Where the on-call duty is remembered, so that the app knows after a reload that it was on. */
export interface DutyStore {
  get(): Promise<boolean>;

  set(on: boolean): Promise<void>;
}

/**
 * Whether the duty is on: `off`; `on`, with the alarm able to sound; or `interrupted`, when the app was reloaded during
 * the duty and needs a tap before the alarm can sound again, as browsers play sound only after one.
 */
export type DutyState = 'off' | 'on' | 'interrupted';

/**
 * The unit's on-call duty, the vehicle mode: while it is on, a new alert sounds the alarm and the screen is kept on.
 * Starting it takes a tap, which lets the alarm play.
 */
export class Duty {

  state: DutyState = 'off';
  /** Whether the screen is kept on, which not every browser can do. */
  screenKeptOn = false;

  constructor(private readonly alarm: Alarm,
              private readonly screen: ScreenLock,
              private readonly store: DutyStore,
              private readonly changed: () => void) {
  }

  /** Finds out whether the duty was on before the app was loaded. */
  async restore() {
    if (await this.store.get()) {
      this.state = 'interrupted';
      this.changed();
    }
  }

  /** Starts the duty, or resumes one that was interrupted. Call it from a tap. */
  async start() {
    await this.alarm.unlock();
    this.state = 'on';
    await this.store.set(true);
    this.screenKeptOn = await this.screen.acquire();
    this.changed();
  }

  async end() {
    this.alarm.stop();
    this.state = 'off';
    this.screenKeptOn = false;
    await this.store.set(false);
    await this.screen.release();
    this.changed();
  }

  /** Takes the screen lock again when the app becomes visible, as the browser let it go when the app was hidden. */
  async visible() {
    if (this.state === 'on') {
      this.screenKeptOn = await this.screen.acquire();
      this.changed();
    }
  }

  /** A new alert came: sounds the alarm while the duty is on. */
  alert() {
    if (this.state === 'on') {
      this.alarm.start();
    }
  }

  /** The alert was acknowledged: silences the alarm. */
  acknowledge() {
    this.alarm.stop();
  }
}

const DUTY = 'duty';

/** Keeps the duty in IndexedDB, beside the paired key. */
export function indexedDbDutyStore(factory: IDBFactory = indexedDB): DutyStore {
  return {
    get: async () => (await run<unknown>(factory, SETTINGS, 'readonly', store => store.get(DUTY))) === true,
    set: async on => {
      await run(factory, SETTINGS, 'readwrite', store => store.put(on, DUTY));
    },
  };
}
