/**
 * The server's API for the mobile app. The app authenticates every call with the key of the destination it is paired
 * with, in the `Authorization` header, never in the address.
 */

/** A state of the unit, with its name in the destination's language. */
export interface State {
  state: string;
  label: string;
}

/** The unit the app is paired with. */
export interface Unit {
  callSign: string;
  /** The language of the destination, `fi` or `sv`. */
  locale: string;
  /** The unit's current state, or `null` if it has none, as when the unit has been deleted. */
  status: State | null;
  /** The states the unit may set next, in the order to show them. */
  nextStates: State[];
  /** The assignment the unit is on now, or `null` if it is on none. */
  assignmentId: number | null;
}

/** A dispatch notification, in the destination's language. */
export interface Notification {
  /** What the app confirms receiving it with. A new one for every dispatch, also to the same assignment again. */
  id: number;
  /** The assignment it was sent for. Alerts stored by an earlier version of the app have none. */
  assignmentId?: number;
  /** When it was sent, as an ISO 8601 instant. */
  timestamp: string;
  urgency: string;
  typeCode: string;
  type: string;
  municipality: string;
  address: string;
  description: string;
  /** The call signs of every resource on the assignment. */
  resources: string[];
}

export type StatusChangeResult =
  | { kind: 'changed'; unit: Unit }
  /** The state may not follow the current one, which `unit` has. */
  | { kind: 'refused'; unit: Unit }
  /** The server does not know the key: the destination got a new key or was deleted. */
  | { kind: 'not-paired' }
  | { kind: 'no-connection' };

/** What a Web Push message carries: the alert, and the language of the destination to show it in. */
export interface PushPayload {
  locale: string;
  notification: Notification;
}

/**
 * The address of the API: the app is served from `<context path>/mobile/` and the API is at `<context path>/api/mobile/`.
 */
export function apiBase(appBase: string = document.baseURI): string {
  return new URL('../api/mobile/', appBase).toString();
}

export function authorization(key: string): Record<string, string> {
  return { Authorization: `Bearer ${key}` };
}

/**
 * Asks the server to set the unit's state. Made once: a change that fails is not tried again later.
 */
export async function changeStatus(key: string, state: string, base: string = apiBase(),
                                   fetchFn: typeof fetch = fetch): Promise<StatusChangeResult> {
  let response: Response;
  try {
    response = await fetchFn(base + 'unit/status', {
      method: 'POST',
      headers: { ...authorization(key), 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ state }),
      cache: 'no-store',
    });
  } catch {
    return { kind: 'no-connection' };
  }
  switch (response.status) {
    case 200:
      return { kind: 'changed', unit: await response.json() as Unit };
    case 409:
      return { kind: 'refused', unit: await response.json() as Unit };
    case 401:
      return { kind: 'not-paired' };
    default:
      return { kind: 'no-connection' };
  }
}

/**
 * Confirms that the app has stored the notification, so that the server stops sending it again.
 *
 * @return whether the server took the confirmation; if not, the server sends the notification again later and the app
 * confirms it then
 */
export async function confirmNotification(key: string, id: number, base: string = apiBase(),
                                          fetchFn: typeof fetch = fetch): Promise<boolean> {
  try {
    const response = await fetchFn(`${base}notifications/${id}/receipt`, {
      method: 'PUT',
      headers: authorization(key),
      cache: 'no-store',
    });
    return response.ok;
  } catch {
    return false;
  }
}

/**
 * The server's public key, which the app subscribes to Web Push with.
 *
 * @return the key, or `null` if the server sends no pushes
 * @throws Error if the server can't be reached
 */
export async function pushKey(key: string, base: string = apiBase(),
                              fetchFn: typeof fetch = fetch): Promise<string | null> {
  const response = await fetchFn(base + 'push/key', {
    headers: { ...authorization(key), Accept: 'application/json' },
    cache: 'no-store',
  });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw new Error(`The push key answered ${response.status}`);
  }
  return (await response.json() as { publicKey: string }).publicKey;
}

/**
 * Tells the server where to push the alerts to: the subscription, as `PushSubscription.toJSON()` gives it.
 *
 * @return whether the server took it
 */
export async function registerPushSubscription(key: string, subscription: PushSubscriptionJSON,
                                               base: string = apiBase(),
                                               fetchFn: typeof fetch = fetch): Promise<boolean> {
  try {
    const response = await fetchFn(base + 'push/subscription', {
      method: 'PUT',
      headers: { ...authorization(key), 'Content-Type': 'application/json' },
      body: JSON.stringify(subscription),
      cache: 'no-store',
    });
    return response.ok;
  } catch {
    return false;
  }
}
