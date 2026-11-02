import { registerSW } from 'virtual:pwa-register';

/** How often an app that stays open, as one mounted in a vehicle does, looks for a new version on the server. */
const CHECK_EVERY_MS = 60 * 60 * 1000;

/**
 * Registers the service worker, which keeps the app shell so that the app opens without a connection, and looks for a
 * new version whenever the app opens and every hour after. A new version takes over at once and the page reloads into
 * it, without reinstalling.
 */
export function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) {
    return;
  }
  registerSW({
    immediate: true,
    onRegisteredSW(_url, registration) {
      if (registration) {
        setInterval(() => void registration.update(), CHECK_EVERY_MS);
      }
    },
  });
}
