import 'fake-indexeddb/auto';
import { IDBFactory } from 'fake-indexeddb';
import { beforeEach, describe, expect, it } from 'vitest';
import { type Address, indexedDbKeyStore, type KeyStore, pair, pairingKeyIn } from './pairing';

const KEY = 'a'.repeat(21) + '_-' + 'Z'.repeat(20);
const OTHER_KEY = 'b'.repeat(43);

describe('the key in a pairing link', () => {
  it('is the pair parameter of the fragment', () => {
    expect(pairingKeyIn(`#pair=${KEY}`)).toBe(KEY);
  });

  it('is nothing when the parameter is not a key', () => {
    expect(pairingKeyIn('#pair=too-short')).toBeNull();
    expect(pairingKeyIn(`#pair=${KEY}=`)).toBeNull();
    expect(pairingKeyIn('#pair=')).toBeNull();
  });

  it('is nothing without the parameter', () => {
    expect(pairingKeyIn('')).toBeNull();
    expect(pairingKeyIn('#other=1')).toBeNull();
  });
});

describe('pairing', () => {
  let store: KeyStore;
  let replacedWith: string | undefined;
  const replace = (url: string) => {
    replacedWith = url;
  };
  const at = (hash: string): Address => ({ pathname: '/idispatch/mobile/', search: '', hash });

  beforeEach(() => {
    // A fresh database for every test, as on a device that has never been paired
    store = indexedDbKeyStore(new IDBFactory());
    replacedWith = undefined;
  });

  it('stores the key of the link and removes it from the address', async () => {
    expect(await pair(at(`#pair=${KEY}`), replace, store)).toBe(KEY);

    expect(await store.get()).toBe(KEY);
    expect(replacedWith).toBe('/idispatch/mobile/');
  });

  it('reads the key before removing it from a live address, like window.location', async () => {
    const live = { pathname: '/idispatch/mobile/', search: '', hash: `#pair=${KEY}` };

    await pair(live, url => {
      live.hash = '';
      replace(url);
    }, store);

    expect(await store.get()).toBe(KEY);
  });

  it('keeps the query of the address', async () => {
    await pair({ pathname: '/idispatch/mobile/', search: '?x=1', hash: `#pair=${KEY}` }, replace, store);

    expect(replacedWith).toBe('/idispatch/mobile/?x=1');
  });

  it('keeps the key it was paired with when opened without a link', async () => {
    await pair(at(`#pair=${KEY}`), replace, store);
    replacedWith = undefined;

    expect(await pair(at(''), replace, store)).toBe(KEY);
    expect(replacedWith).toBeUndefined();
  });

  it('replaces an earlier pairing with a new link', async () => {
    await pair(at(`#pair=${KEY}`), replace, store);

    expect(await pair(at(`#pair=${OTHER_KEY}`), replace, store)).toBe(OTHER_KEY);
  });

  it('is not paired without a link', async () => {
    expect(await pair(at(''), replace, store)).toBeNull();
  });

  it('removes a broken link from the address, but keeps the earlier pairing', async () => {
    await pair(at(`#pair=${KEY}`), replace, store);
    replacedWith = undefined;

    expect(await pair(at('#pair=broken'), replace, store)).toBe(KEY);
    expect(replacedWith).toBe('/idispatch/mobile/');
  });
});
