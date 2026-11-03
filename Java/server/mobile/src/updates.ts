import { Workbox } from 'workbox-window';

/** How often an app that stays open, as one mounted in a vehicle does, looks for a new version on the server. */
const CHECK_EVERY_MS = 60 * 60 * 1000;

/** What {@link Updates} needs of the service worker; Workbox's `Workbox` in the app, a fake in tests. */
export interface ServiceWorkerRegistrar {
  register(): Promise<unknown>;

  update(): Promise<unknown>;

  addEventListener(type: 'controlling', listener: (event: { isUpdate?: boolean }) => void): void;
}

/**
 * Installs new versions of the app. The service worker keeps the app shell, so that the app opens without a connection,
 * and looks for a new version whenever the app opens and every hour after. A new version takes over at once, and the
 * page reloads into it, without reinstalling, but not while {@link mayReload} says no, as while the unit is on call: a
 * reload would silence the alarm until someone taps the screen again. It waits until {@link reloadIfWaiting} is called
 * then, and the page goes on with the files it has loaded.
 */
export class Updates {

  /** Whether a new version has taken over, waiting for the app to allow a reload. */
  waiting = false;

  constructor(private readonly mayReload: () => boolean,
              private readonly changed: () => void,
              private readonly reload: () => void = () => window.location.reload(),
              private readonly registrar: () => ServiceWorkerRegistrar | null = defaultRegistrar) {
  }

  register() {
    const registrar = this.registrar();
    if (registrar === null) {
      return;
    }
    registrar.addEventListener('controlling', event => {
      // The first version taking over the page that installed it is no update
      if (event.isUpdate) {
        this.waiting = true;
        this.changed();
        this.reloadIfWaiting();
      }
    });
    void registrar.register();
    setInterval(() => void registrar.update(), CHECK_EVERY_MS);
  }

  /** Reloads into the new version if one is waiting and the app may reload now. */
  reloadIfWaiting() {
    if (this.waiting && this.mayReload()) {
      this.waiting = false;
      this.reload();
    }
  }
}

function defaultRegistrar(): ServiceWorkerRegistrar | null {
  return 'serviceWorker' in navigator ? new Workbox('./sw.js', { scope: './' }) : null;
}
