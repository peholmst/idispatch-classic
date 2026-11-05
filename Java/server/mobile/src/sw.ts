/// <reference lib="webworker" />
import { clientsClaim } from 'workbox-core';
import { cleanupOutdatedCaches, createHandlerBoundToURL, precacheAndRoute } from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';
import { apiBase, confirmNotification, type PushPayload, registerPushSubscription } from './api';
import { MessageStore } from './messages';
import { indexedDbKeyStore } from './pairing';
import { receivePush } from './push';

/**
 * The app's service worker. It keeps the app shell, so that the app opens without a connection, and receives the
 * alerts by Web Push while the app is in the background or closed (see `push.ts`).
 */
declare const self: ServiceWorkerGlobalScope;

// The app shell: the page, its script, styles and font, and the icons. A new version takes over at once, so that it
// also reaches apps installed before the app decided when to reload, and the first one takes over the page that
// installed it, so that the app opens offline from the start. The page goes on with the files it has loaded until it
// reloads (updates.ts).
precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();
registerRoute(new NavigationRoute(createHandlerBoundToURL('index.html')));
void self.skipWaiting();
clientsClaim();

/** The API, relative to where the app is served, as in the app. */
const api = () => apiBase(self.registration.scope);

self.addEventListener('push', event => {
  let payload: PushPayload;
  try {
    payload = event.data?.json() as PushPayload;
  } catch {
    return;
  }
  if (payload?.notification === undefined) {
    return;
  }
  const store = new MessageStore(indexedDB);
  const keys = indexedDbKeyStore(indexedDB);
  event.waitUntil(receivePush(payload, {
    store: notification => store.add(notification),
    key: () => keys.get(),
    confirm: (key, id) => confirmNotification(key, id, api()),
    windows: async () => (await self.clients.matchAll({ type: 'window', includeUncontrolled: true }))
      .map(client => ({
        visible: (client as WindowClient).visibilityState === 'visible',
        postMessage: message => client.postMessage(message),
      })),
    showNotification: (title, options) => self.registration.showNotification(title, options),
  }));
});

// Tapping the notification opens the app on its alerts, in the window that is open if there is one
self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
    const open = windows[0] as WindowClient | undefined;
    if (open !== undefined) {
      await open.focus();
    } else {
      await self.clients.openWindow(self.registration.scope);
    }
  })());
});

// The push service replaced the subscription, as when it expired: subscribe again with the same server key, and tell
// the server, so that the alerts keep coming while the app stays closed
self.addEventListener('pushsubscriptionchange', event => {
  const change = event as PushSubscriptionChangeEvent;
  event.waitUntil((async () => {
    const serverKey = change.oldSubscription?.options.applicationServerKey;
    const subscription = change.newSubscription
      ?? (serverKey ? await self.registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: serverKey })
        : null);
    const key = await indexedDbKeyStore(indexedDB).get();
    if (subscription !== null && key !== null) {
      await registerPushSubscription(key, subscription.toJSON(), api());
    }
  })());
});

/** Not in TypeScript's types yet. */
interface PushSubscriptionChangeEvent extends ExtendableEvent {
  readonly oldSubscription: PushSubscription | null;
  readonly newSubscription: PushSubscription | null;
}
