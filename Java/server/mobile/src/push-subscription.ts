import { pushKey, registerPushSubscription } from './api';

/**
 * Whether the app gets alerts by Web Push when it is in the background or closed: `on` once the server has its
 * subscription, `ask` while the user hasn't been asked for permission to show notifications, `denied` once they said
 * no, and `failed` when subscribing or telling the server failed, to be tried again. `unavailable` when the browser
 * can't, or the server sends no pushes.
 */
export type PushState = 'on' | 'ask' | 'denied' | 'failed' | 'unavailable';

/** What subscribing needs of the browser; the real one in the app, a fake in tests. */
export interface PushBrowser {
  permission(): NotificationPermission;

  requestPermission(): Promise<NotificationPermission>;

  /** The service worker's push manager, once it is active, or `null` if the browser has no Web Push. */
  pushManager(): Promise<PushManager | null>;
}

/**
 * Subscribes the app to Web Push with the server's key and gives the server the subscription, once the user has let the
 * app show notifications. Done whenever the app opens, so that the server has the subscription again after the
 * destination got a new key and the app was paired again, and so that a subscription made with an old server key is
 * replaced.
 */
export class PushSetup {

  constructor(private readonly key: string,
              private readonly browser: PushBrowser = defaultBrowser(),
              private readonly api: {
                pushKey: typeof pushKey;
                register: typeof registerPushSubscription;
              } = { pushKey, register: registerPushSubscription }) {
  }

  /** Subscribes if the user has already let the app show notifications. */
  async refresh(): Promise<PushState> {
    const manager = await this.browser.pushManager();
    if (manager === null) {
      return 'unavailable';
    }
    switch (this.browser.permission()) {
      case 'default':
        return 'ask';
      case 'denied':
        return 'denied';
    }
    try {
      const serverKey = await this.api.pushKey(this.key);
      if (serverKey === null) {
        return 'unavailable';
      }
      const subscription = await subscribe(manager, serverKey);
      return await this.api.register(this.key, subscription.toJSON()) ? 'on' : 'failed';
    } catch {
      return 'failed';
    }
  }

  /** Asks the user to let the app show notifications, and subscribes if they do. Must follow a tap. */
  async enable(): Promise<PushState> {
    await this.browser.requestPermission();
    return this.refresh();
  }
}

async function subscribe(manager: PushManager, serverKey: string): Promise<PushSubscription> {
  const key = base64UrlToBytes(serverKey);
  const existing = await manager.getSubscription();
  if (existing !== null) {
    const existingKey = existing.options.applicationServerKey;
    if (existingKey !== null && sameBytes(new Uint8Array(existingKey), key)) {
      return existing;
    }
    // Made with another server key, which the push service would refuse the server's messages for
    await existing.unsubscribe();
  }
  return manager.subscribe({ userVisibleOnly: true, applicationServerKey: key });
}

export function base64UrlToBytes(value: string): Uint8Array<ArrayBuffer> {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(base64 + '='.repeat((4 - base64.length % 4) % 4));
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function sameBytes(a: Uint8Array, b: Uint8Array) {
  return a.length === b.length && a.every((byte, i) => byte === b[i]);
}

function defaultBrowser(): PushBrowser {
  const supported = 'serviceWorker' in navigator && 'PushManager' in window && 'Notification' in window;
  return {
    permission: () => Notification.permission,
    requestPermission: () => Notification.requestPermission(),
    pushManager: async () => supported ? (await navigator.serviceWorker.ready).pushManager : null,
  };
}
