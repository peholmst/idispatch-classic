import { describe, expect, it, vi } from 'vitest';
import { fetchUnit } from './api';

const BASE = 'https://example.com/idispatch/api/mobile/';
const KEY = 'k'.repeat(43);

function responding(status: number, body: unknown = {}) {
  return vi.fn(async (_url: RequestInfo | URL, _init?: RequestInit) =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }));
}

describe('fetching the unit', () => {
  it('sends the key in the Authorization header, not in the address', async () => {
    const fetchFn = responding(200, { callSign: 'RVS911', locale: 'fi', status: null });

    await fetchUnit(KEY, BASE, fetchFn);

    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe(BASE + 'unit');
    expect(String(url)).not.toContain(KEY);
    expect(new Headers(init?.headers).get('Authorization')).toBe(`Bearer ${KEY}`);
  });

  it('returns the unit', async () => {
    const unit = { callSign: 'RVS911', locale: 'sv', status: { state: 'AVAILABLE', label: 'Tillgänglig' } };

    expect(await fetchUnit(KEY, BASE, responding(200, unit))).toEqual({ kind: 'unit', unit });
  });

  it('is not paired when the server does not know the key', async () => {
    expect(await fetchUnit(KEY, BASE, responding(401))).toEqual({ kind: 'not-paired' });
  });

  it('has no connection when the server cannot be reached or fails', async () => {
    const unreachable = vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    });

    expect(await fetchUnit(KEY, BASE, unreachable)).toEqual({ kind: 'no-connection' });
    expect(await fetchUnit(KEY, BASE, responding(502))).toEqual({ kind: 'no-connection' });
  });
});
