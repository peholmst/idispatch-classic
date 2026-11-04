import type { Notification, PushPayload } from './api';
import { formatTime } from './format';
import { textsFor } from './texts';

/**
 * What the service worker tells the app's windows when a push brings an alert: `isNew` unless the app had already
 * stored it, as when it came over the event stream first.
 */
export interface AlertMessage {
  type: 'alert';
  notification: Notification;
  isNew: boolean;
}

/** An open window of the app, as the service worker sees it. */
export interface AppWindow {
  readonly visible: boolean;

  postMessage(message: AlertMessage): void;
}

/** What handling a push needs of the service worker; the real one in `sw.ts`, fakes in tests. */
export interface PushContext {
  /** Stores the alert, returning whether it was new (`MessageStore.add`). */
  store(notification: Notification): Promise<boolean>;

  /** The key the app is paired with, or `null`. */
  key(): Promise<string | null>;

  /** Confirms the alert to the server, which stores a receipt. */
  confirm(key: string, id: number): Promise<boolean>;

  windows(): Promise<AppWindow[]>;

  showNotification(title: string, options: NotificationOptions & { vibrate?: number[] }): Promise<void>;
}

/** A long, insistent vibration: four long pulses. */
export const VIBRATION = [800, 300, 800, 300, 800, 300, 800];

/**
 * Handles a push that brings an alert, with the app in the background or closed: stores the alert, as the event
 * stream would, so that it is in the message view when the app opens, confirms it to the server, which stores a
 * receipt even if the app is never opened, and tells the open windows of the app about it.
 * <p>
 * Shows it as a notification that vibrates and stays until it is tapped or swiped away, unless a window of the app is
 * visible, which shows the alert itself. It is shown also when the app had already stored the alert, as one in the
 * background gets the alert over the event stream without anyone seeing it. Notifications are tagged with the alert,
 * so that one coming again replaces the first.
 */
export async function receivePush(payload: PushPayload, context: PushContext): Promise<void> {
  const { notification, locale } = payload;
  const isNew = await context.store(notification);
  const windows = await context.windows();
  windows.forEach(window => window.postMessage({ type: 'alert', notification, isNew }));
  const tasks: Promise<unknown>[] = [];
  if (!windows.some(window => window.visible)) {
    const t = textsFor(locale);
    tasks.push(context.showNotification(`${t.newAlert}: ${notification.urgency} ${notification.typeCode} ${notification.type}`, {
      body: [`${notification.address}, ${notification.municipality}`, notification.description,
        `${t.resources}: ${notification.resources.join(', ')} – ${formatTime(notification.timestamp, locale)}`]
        .filter(line => line !== '')
        .join('\n'),
      lang: locale,
      tag: `alert-${notification.id}`,
      requireInteraction: true,
      vibrate: VIBRATION,
      icon: 'icon-192.png',
      data: { id: notification.id },
    }));
  }
  const key = await context.key();
  if (key !== null) {
    tasks.push(context.confirm(key, notification.id));
  }
  await Promise.all(tasks);
}

/** Whether the message from the service worker tells about an alert. */
export function isAlertMessage(data: unknown): data is AlertMessage {
  return typeof data === 'object' && data !== null && (data as AlertMessage).type === 'alert'
    && typeof (data as AlertMessage).notification === 'object';
}
