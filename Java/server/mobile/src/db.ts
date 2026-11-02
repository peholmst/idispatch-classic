/**
 * The app's IndexedDB database, which the browser tab and the installed app share: the paired key in `settings`, and
 * the alerts in `messages`.
 */
export const DATABASE = 'idispatch-mobile';
export const SETTINGS = 'settings';
export const MESSAGES = 'messages';

/** Version 1 held only the settings; version 2 added the messages. */
const VERSION = 2;

export function openDatabase(factory: IDBFactory): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = factory.open(DATABASE, VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(SETTINGS)) {
        db.createObjectStore(SETTINGS);
      }
      if (!db.objectStoreNames.contains(MESSAGES)) {
        db.createObjectStore(MESSAGES, { keyPath: 'id' });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

/**
 * Runs one request in a transaction of its own on the store, and returns its result once the transaction is done.
 */
export async function run<T>(factory: IDBFactory, storeName: string, mode: IDBTransactionMode,
                             action: (store: IDBObjectStore) => IDBRequest<T>): Promise<T> {
  const db = await openDatabase(factory);
  try {
    return await new Promise<T>((resolve, reject) => {
      const transaction = db.transaction(storeName, mode);
      const request = action(transaction.objectStore(storeName));
      transaction.oncomplete = () => resolve(request.result);
      transaction.onerror = () => reject(transaction.error);
    });
  } finally {
    db.close();
  }
}
