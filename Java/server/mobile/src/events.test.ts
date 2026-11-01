import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Notification, Unit } from './api';
import { type ConnectionState, EventParser, RETRY_DELAYS_MS, UnitConnection } from './events';

const KEY = 'k'.repeat(43);
const URL = 'https://example.com/idispatch/api/mobile/events';
const UNIT: Unit = { callSign: 'RVS911', locale: 'fi', status: null, nextStates: [] };
const NOTIFICATION: Notification = {
  id: 1, timestamp: '2026-11-01T12:00:00Z', urgency: 'A', typeCode: '402', type: 'Rakennuspalo',
  municipality: 'Parainen', address: 'Rantatie 1', description: '', resources: ['RVS911'],
};

describe('reading Server-Sent Events', () => {
  it('splits events, whatever pieces the text arrives in', () => {
    const parser = new EventParser();

    expect(parser.push('event:unit\nda')).toEqual([]);
    expect(parser.push('ta:{"a":1}\n\n:heartbeat\n\nevent: unit\r\ndata: 2\r\n\r\n')).toEqual([
      { event: 'unit', data: '{"a":1}' },
      { event: 'unit', data: '2' },
    ]);
  });

  it('joins the data lines of an event', () => {
    expect(new EventParser().push('data:a\ndata:b\n\n')).toEqual([{ event: 'message', data: 'a\nb' }]);
  });
});

/** A response whose body the test writes to, as the server would. */
function openStream() {
  let controller!: ReadableStreamDefaultController<Uint8Array>;
  const body = new ReadableStream<Uint8Array>({ start: c => void (controller = c) });
  const encoder = new TextEncoder();
  return {
    response: new Response(body, { status: 200, headers: { 'Content-Type': 'text/event-stream' } }),
    send: (unit: Unit) => controller.enqueue(encoder.encode(`event:unit\ndata:${JSON.stringify(unit)}\n\n`)),
    sendNotification: (notification: Notification) => controller.enqueue(encoder.encode(
      `event:notification\nid:${notification.id}\ndata:${JSON.stringify(notification)}\n\n`)),
    end: () => controller.close(),
  };
}

describe('the connection to the server', () => {
  let states: ConnectionState[];
  let units: Unit[];
  let notifications: Notification[];
  const listener = {
    unit: (unit: Unit) => void units.push(unit),
    notification: (notification: Notification) => void notifications.push(notification),
    state: (state: ConnectionState) => void states.push(state),
  };

  beforeEach(() => {
    vi.useFakeTimers();
    states = [];
    units = [];
    notifications = [];
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('opens the stream with the key in the Authorization header, and passes on every unit', async () => {
    const stream = openStream();
    const fetchFn = vi.fn(async (_url: RequestInfo | URL, _init?: RequestInit) => stream.response);
    const connection = new UnitConnection(KEY, URL, listener, fetchFn);

    connection.start();
    stream.send(UNIT);
    stream.send({ ...UNIT, callSign: 'RVS912' });
    await vi.advanceTimersByTimeAsync(0);

    expect(new Headers(fetchFn.mock.calls[0][1]?.headers).get('Authorization')).toBe(`Bearer ${KEY}`);
    expect(states).toEqual(['connecting', 'connected']);
    expect(units.map(unit => unit.callSign)).toEqual(['RVS911', 'RVS912']);
    connection.stop();
  });

  it('passes on the notifications', async () => {
    const stream = openStream();
    const connection = new UnitConnection(KEY, URL, listener, vi.fn(async () => stream.response));
    const notification = { ...NOTIFICATION, id: 7 };

    connection.start();
    stream.send(UNIT);
    stream.sendNotification(notification);
    await vi.advanceTimersByTimeAsync(0);

    expect(notifications).toEqual([notification]);
    connection.stop();
  });

  it('is disconnected while the server cannot be reached, and tries again ever more slowly', async () => {
    const fetchFn = vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    });
    const connection = new UnitConnection(KEY, URL, listener, fetchFn);

    connection.start();
    await vi.advanceTimersByTimeAsync(0);
    expect(states).toEqual(['connecting', 'disconnected']);

    await vi.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
    expect(fetchFn).toHaveBeenCalledTimes(2);
    await vi.advanceTimersByTimeAsync(RETRY_DELAYS_MS[1] - 1);
    expect(fetchFn).toHaveBeenCalledTimes(2);
    await vi.advanceTimersByTimeAsync(1);
    expect(fetchFn).toHaveBeenCalledTimes(3);
    connection.stop();
  });

  it('connects again when the stream ends', async () => {
    const first = openStream();
    const second = openStream();
    const fetchFn = vi.fn().mockResolvedValueOnce(first.response).mockResolvedValueOnce(second.response);
    const connection = new UnitConnection(KEY, URL, listener, fetchFn);
    connection.start();
    first.send(UNIT);
    await vi.advanceTimersByTimeAsync(0);

    first.end();
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toBe('disconnected');

    await vi.advanceTimersByTimeAsync(RETRY_DELAYS_MS[0]);
    second.send(UNIT);
    await vi.advanceTimersByTimeAsync(0);
    expect(states.at(-1)).toBe('connected');
    connection.stop();
  });

  it('connects again right away when asked, as when the device comes back online', async () => {
    const fetchFn = vi.fn(async () => {
      throw new TypeError('Failed to fetch');
    });
    const connection = new UnitConnection(KEY, URL, listener, fetchFn);
    connection.start();
    await vi.advanceTimersByTimeAsync(0);

    connection.reconnectNow();
    await vi.advanceTimersByTimeAsync(0);

    expect(fetchFn).toHaveBeenCalledTimes(2);
    connection.stop();
  });

  it('stops trying once the server refuses the key', async () => {
    const fetchFn = vi.fn(async () => new Response(null, { status: 401 }));
    const connection = new UnitConnection(KEY, URL, listener, fetchFn);

    connection.start();
    await vi.advanceTimersByTimeAsync(60_000);

    expect(states).toEqual(['connecting', 'not-paired']);
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });
});
