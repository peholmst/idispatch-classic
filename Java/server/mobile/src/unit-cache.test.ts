import 'fake-indexeddb/auto';
import { IDBFactory } from 'fake-indexeddb';
import { describe, expect, it } from 'vitest';
import type { Unit } from './api';
import { indexedDbKeyStore } from './pairing';
import { UnitCache } from './unit-cache';

const UNIT: Unit = {
  callSign: 'RVS911', locale: 'sv', status: { state: 'AVAILABLE', label: 'Ledig' },
  nextStates: [{ state: 'AT_STATION', label: 'På stationen' }], assignmentId: null,
};

describe('the last known unit', () => {
  it('is nothing before the server has told it', async () => {
    expect(await new UnitCache(new IDBFactory()).get()).toBeNull();
  });

  it('is the one the server told last, after a reload', async () => {
    const factory = new IDBFactory();
    await new UnitCache(factory).set({ ...UNIT, callSign: 'old' });
    await new UnitCache(factory).set(UNIT);

    expect(await new UnitCache(factory).get()).toEqual(UNIT);
  });

  it('is kept beside the paired key without touching it', async () => {
    const factory = new IDBFactory();
    await indexedDbKeyStore(factory).set('k'.repeat(43));

    await new UnitCache(factory).set(UNIT);

    expect(await indexedDbKeyStore(factory).get()).toBe('k'.repeat(43));
  });
});
