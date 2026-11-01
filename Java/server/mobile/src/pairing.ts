/**
 * Pairing the app with a mobile destination. The administrator shows the QR code of the pairing link
 * `<server>/idispatch/mobile/#pair=<key>`; opening it stores the key, and removes it from the address so that it does
 * not stay in the browser's history or get shared along with the address. The key is in the fragment, which browsers
 * never send to the server.
 */

/** A key is 256 random bits, base64url-encoded without padding, as the server generates it. */
const KEY_FORMAT = /^[A-Za-z0-9_-]{43}$/;

/**
 * Returns the key in the `pair` parameter of the fragment, or `null` if there is none or it is not a key.
 */
export function pairingKeyIn(fragment: string): string | null {
  const key = pairingParameters(fragment).get('pair');
  return key !== null && KEY_FORMAT.test(key) ? key : null;
}

function pairingParameters(fragment: string): URLSearchParams {
  return new URLSearchParams(fragment.startsWith('#') ? fragment.slice(1) : fragment);
}

/** Where the app keeps the key of the destination it is paired with. */
export interface KeyStore {
  get(): Promise<string | null>;

  set(key: string): Promise<void>;
}

/** The parts of `window.location` that pairing reads. */
export interface Address {
  readonly pathname: string;
  readonly search: string;
  readonly hash: string;
}

/**
 * Pairs the app with the key in the address, if there is one, and returns the key the app is paired with, or `null` if
 * it is not paired. A pairing link replaces the key of an earlier pairing. Any `pair` parameter, even one that is not a
 * key, is removed from the address with `replaceAddress`, which takes the address to show instead.
 */
export async function pair(address: Address, replaceAddress: (url: string) => void,
                           store: KeyStore): Promise<string | null> {
  // Read before replacing it, as the address may be the live window.location
  const fragment = address.hash;
  if (pairingParameters(fragment).has('pair')) {
    replaceAddress(address.pathname + address.search);
    const key = pairingKeyIn(fragment);
    if (key !== null) {
      await store.set(key);
    }
  }
  return store.get();
}

const DATABASE = 'idispatch-mobile';
const STORE = 'settings';
const KEY = 'key';

/**
 * Keeps the key in IndexedDB, which the browser tab and the installed app share, so that pairing in the browser before
 * installing works.
 */
export function indexedDbKeyStore(factory: IDBFactory = indexedDB): KeyStore {
  const open = () => new Promise<IDBDatabase>((resolve, reject) => {
    const request = factory.open(DATABASE, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  const run = async <T>(mode: IDBTransactionMode, action: (store: IDBObjectStore) => IDBRequest<T>) => {
    const db = await open();
    try {
      return await new Promise<T>((resolve, reject) => {
        const transaction = db.transaction(STORE, mode);
        const request = action(transaction.objectStore(STORE));
        transaction.oncomplete = () => resolve(request.result);
        transaction.onerror = () => reject(transaction.error);
      });
    } finally {
      db.close();
    }
  };
  return {
    get: async () => (await run<unknown>('readonly', store => store.get(KEY)) as string | undefined) ?? null,
    set: async key => {
      await run('readwrite', store => store.put(key, KEY));
    },
  };
}
