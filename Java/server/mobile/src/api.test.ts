import { describe, expect, it, vi } from 'vitest';
import { changeStatus, type Unit } from './api';

const BASE = 'https://example.com/idispatch/api/mobile/';
const KEY = 'k'.repeat(43);
const UNIT: Unit = {
  callSign: 'RVS911', locale: 'fi', status: { state: 'AVAILABLE', label: 'Vapaa' },
  nextStates: [{ state: 'AT_STATION', label: 'Asemalla' }],
};

function responding(status: number, body: unknown = {}) {
  return vi.fn(async (_url: RequestInfo | URL, _init?: RequestInit) =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));
}

describe('changing the status', () => {
  it('posts the state with the key in the Authorization header, not in the address', async () => {
    const fetchFn = responding(200, UNIT);

    await changeStatus(KEY, 'AVAILABLE', BASE, fetchFn);

    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(BASE + 'unit/status');
    expect(String(url)).not.toContain(KEY);
    expect(init?.method).toBe('POST');
    expect(init?.body).toBe(JSON.stringify({ state: 'AVAILABLE' }));
    expect(new Headers(init?.headers).get('Authorization')).toBe(`Bearer ${KEY}`);
  });

  it('returns the unit with its new status', async () => {
    expect(await changeStatus(KEY, 'AVAILABLE', BASE, responding(200, UNIT))).toEqual({ kind: 'changed', unit: UNIT });
  });

  it('is refused with the status the unit has when the state may not follow it', async () => {
    expect(await changeStatus(KEY, 'ON_SCENE', BASE, responding(409, UNIT))).toEqual({ kind: 'refused', unit: UNIT });
  });

  it('is not paired when the server does not know the key', async () => {
    expect(await changeStatus(KEY, 'AVAILABLE', BASE, responding(401))).toEqual({ kind: 'not-paired' });
  });

  it('fails without a connection, and is made only once', async () => {
    const unreachable = vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    });

    expect(await changeStatus(KEY, 'AVAILABLE', BASE, unreachable)).toEqual({ kind: 'no-connection' });
    expect(unreachable).toHaveBeenCalledTimes(1);
    expect(await changeStatus(KEY, 'AVAILABLE', BASE, responding(502))).toEqual({ kind: 'no-connection' });
  });
});
