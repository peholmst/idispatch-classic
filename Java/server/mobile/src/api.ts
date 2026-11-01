/**
 * The server's API for the mobile app. The app authenticates every call with the key of the destination it is paired
 * with, in the `Authorization` header, never in the address.
 */

/** The unit the app is paired with. */
export interface Unit {
  callSign: string;
  /** The language of the destination, `fi` or `sv`. */
  locale: string;
  /** The unit's current status, or `null` if it has none, as when the unit has been deleted. */
  status: { state: string; label: string } | null;
}

export type UnitResult =
  | { kind: 'unit'; unit: Unit }
  /** The server does not know the key: the destination got a new key or was deleted. */
  | { kind: 'not-paired' }
  | { kind: 'no-connection' };

/**
 * The address of the API: the app is served from `<context path>/mobile/` and the API is at `<context path>/api/mobile/`.
 */
export function apiBase(): string {
  return new URL('../api/mobile/', document.baseURI).toString();
}

export async function fetchUnit(key: string, base: string = apiBase(),
                                fetchFn: typeof fetch = fetch): Promise<UnitResult> {
  let response: Response;
  try {
    response = await fetchFn(base + 'unit', {
      headers: { Authorization: `Bearer ${key}`, Accept: 'application/json' },
      cache: 'no-store',
    });
  } catch {
    return { kind: 'no-connection' };
  }
  if (response.status === 401) {
    return { kind: 'not-paired' };
  }
  if (!response.ok) {
    return { kind: 'no-connection' };
  }
  return { kind: 'unit', unit: await response.json() as Unit };
}
