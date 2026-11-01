import 'fake-indexeddb/auto';
import { IDBFactory } from 'fake-indexeddb';
import { beforeEach, describe, expect, it } from 'vitest';
import type { Notification } from './api';
import { formatTime } from './format';
import { MessageStore, REMEMBER_DELETED_MS } from './messages';
import { indexedDbKeyStore } from './pairing';

function notification(id: number, timestamp: string): Notification {
  return {
    id, timestamp, urgency: 'A', typeCode: '402', type: 'Rakennuspalo', municipality: 'Parainen',
    address: `Rantatie ${id}`, description: '', resources: ['RVS911'],
  };
}

describe('the stored alerts', () => {
  let factory: IDBFactory;
  let now: number;
  let store: MessageStore;

  beforeEach(() => {
    factory = new IDBFactory();
    now = Date.parse('2026-11-01T12:00:00Z');
    store = new MessageStore(factory, () => now);
  });

  it('are listed newest first', async () => {
    await store.add(notification(1, '2026-11-01T10:00:00Z'));
    await store.add(notification(3, '2026-11-01T11:00:00Z'));
    await store.add(notification(2, '2026-11-01T09:00:00Z'));

    expect((await store.list()).map(it => it.id)).toEqual([3, 1, 2]);
  });

  it('survive a reload', async () => {
    await store.add(notification(1, '2026-11-01T10:00:00Z'));

    expect((await new MessageStore(factory).list()).map(it => it.id)).toEqual([1]);
  });

  it('are stored once, however many times the server sends them', async () => {
    expect(await store.add(notification(1, '2026-11-01T10:00:00Z'))).toBe(true);
    expect(await store.add(notification(1, '2026-11-01T10:00:00Z'))).toBe(false);

    expect(await store.list()).toHaveLength(1);
  });

  it('stay deleted when the server sends them again', async () => {
    await store.add(notification(1, '2026-11-01T10:00:00Z'));

    await store.delete(1);

    expect(await store.list()).toEqual([]);
    expect(await store.add(notification(1, '2026-11-01T10:00:00Z'))).toBe(false);
    expect(await store.list()).toEqual([]);
  });

  it('are forgotten a while after they were deleted', async () => {
    await store.add(notification(1, '2026-11-01T10:00:00Z'));
    await store.delete(1);

    now += REMEMBER_DELETED_MS + 1;
    await store.forgetOldDeleted();

    expect(await store.add(notification(1, '2026-11-01T10:00:00Z'))).toBe(true);
  });

  it('share the database with the paired key, which an earlier version of the app created', async () => {
    // Version 1 of the database held only the key
    await new Promise<void>((resolve, reject) => {
      const request = factory.open('idispatch-mobile', 1);
      request.onupgradeneeded = () => request.result.createObjectStore('settings');
      request.onsuccess = () => {
        const tx = request.result.transaction('settings', 'readwrite');
        tx.objectStore('settings').put('k'.repeat(43), 'key');
        tx.oncomplete = () => {
          request.result.close();
          resolve();
        };
      };
      request.onerror = () => reject(request.error);
    });

    await store.add(notification(1, '2026-11-01T10:00:00Z'));

    expect(await indexedDbKeyStore(factory).get()).toBe('k'.repeat(43));
    expect(await store.list()).toHaveLength(1);
  });
});

describe('the time of an alert', () => {
  const now = new Date(2026, 10, 1, 15, 0);

  it('is the time alone today', () => {
    expect(formatTime(new Date(2026, 10, 1, 9, 5).toISOString(), 'fi', now)).toBe('09.05');
  });

  it('has the date on other days', () => {
    expect(formatTime(new Date(2026, 9, 31, 9, 5).toISOString(), 'fi', now)).toContain('31.10.');
  });
});
