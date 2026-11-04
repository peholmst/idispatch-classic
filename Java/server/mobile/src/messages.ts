import type { Notification } from './api';
import { MESSAGES, run } from './db';

/** An alert as the app keeps it. A deleted one is kept for a while, so that it is not stored again if it comes again. */
interface StoredMessage extends Notification {
  deleted: boolean;
  /** When it was deleted, in milliseconds since the epoch, or 0. */
  deletedAt: number;
}

/** How long a deleted alert is remembered, which is well over the server's redelivery window. */
export const REMEMBER_DELETED_MS = 7 * 24 * 60 * 60 * 1000;

/**
 * The alerts the app has received, in IndexedDB, so that they survive a reload. An alert stays until the user deletes
 * it.
 */
export class MessageStore {

  constructor(private readonly factory: IDBFactory = indexedDB,
              private readonly now: () => number = Date.now) {
  }

  /** The alerts not deleted, newest first. */
  async list(): Promise<Notification[]> {
    const all = await run<StoredMessage[]>(this.factory, MESSAGES, 'readonly', store => store.getAll());
    return all
      .filter(message => !message.deleted)
      .sort((a, b) => b.timestamp.localeCompare(a.timestamp) || b.id - a.id)
      .map(({ deleted: _deleted, deletedAt: _deletedAt, ...notification }) => notification);
  }

  /**
   * Stores the alert, unless it is already stored or was deleted, as when the server sends it again, or when it comes
   * both over the event stream and by push. Checked and stored in one transaction, so that the app and its service
   * worker storing the same alert at once find it new only once.
   *
   * @return whether it was new
   */
  async add(notification: Notification): Promise<boolean> {
    try {
      await run(this.factory, MESSAGES, 'readwrite',
        store => store.add({ ...notification, deleted: false, deletedAt: 0 } satisfies StoredMessage));
      return true;
    } catch (error) {
      if (error instanceof DOMException && error.name === 'ConstraintError') {
        return false;
      }
      throw error;
    }
  }

  async delete(id: number): Promise<void> {
    const existing = await run<StoredMessage | undefined>(this.factory, MESSAGES, 'readonly', store => store.get(id));
    if (existing !== undefined) {
      await run(this.factory, MESSAGES, 'readwrite',
        store => store.put({ ...existing, deleted: true, deletedAt: this.now() }));
    }
  }

  /** Forgets the alerts deleted longer ago than {@link REMEMBER_DELETED_MS}. */
  async forgetOldDeleted(): Promise<void> {
    const all = await run<StoredMessage[]>(this.factory, MESSAGES, 'readonly', store => store.getAll());
    const limit = this.now() - REMEMBER_DELETED_MS;
    for (const message of all.filter(it => it.deleted && it.deletedAt < limit)) {
      await run(this.factory, MESSAGES, 'readwrite', store => store.delete(message.id));
    }
  }
}
