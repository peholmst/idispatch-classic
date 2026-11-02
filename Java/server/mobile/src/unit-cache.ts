import type { Unit } from './api';
import { run, SETTINGS } from './db';

const UNIT = 'unit';

/**
 * The unit as the server last told it, so that an app opened without a connection shows the last known status. It is
 * only shown: the status buttons stay disabled until the server answers, and the server's word replaces it then.
 */
export class UnitCache {

  constructor(private readonly factory: IDBFactory = indexedDB) {
  }

  async get(): Promise<Unit | null> {
    return (await run<unknown>(this.factory, SETTINGS, 'readonly', store => store.get(UNIT)) as Unit | undefined)
      ?? null;
  }

  async set(unit: Unit): Promise<void> {
    await run(this.factory, SETTINGS, 'readwrite', store => store.put(unit, UNIT));
  }
}
