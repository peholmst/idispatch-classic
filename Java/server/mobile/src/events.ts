import { authorization, type Notification, type Unit } from './api';

/** One Server-Sent Event. */
export interface ServerEvent {
  event: string;
  data: string;
}

/**
 * Splits a stream of Server-Sent Events into events, as text arrives in arbitrary pieces. Comments, such as the
 * server's heartbeats, and fields other than `event` and `data` are skipped.
 */
export class EventParser {

  private buffer = '';
  private event = 'message';
  private data: string[] = [];

  push(text: string): ServerEvent[] {
    this.buffer += text;
    const events: ServerEvent[] = [];
    let end: number;
    while ((end = this.buffer.search(/\r\n|\r|\n/)) >= 0) {
      const line = this.buffer.slice(0, end);
      const separator = this.buffer.startsWith('\r\n', end) ? 2 : 1;
      this.buffer = this.buffer.slice(end + separator);
      if (line === '') {
        if (this.data.length > 0) {
          events.push({ event: this.event, data: this.data.join('\n') });
        }
        this.event = 'message';
        this.data = [];
      } else if (!line.startsWith(':')) {
        const colon = line.indexOf(':');
        const field = colon < 0 ? line : line.slice(0, colon);
        let value = colon < 0 ? '' : line.slice(colon + 1);
        if (value.startsWith(' ')) {
          value = value.slice(1);
        }
        if (field === 'event') {
          this.event = value;
        } else if (field === 'data') {
          this.data.push(value);
        }
      }
    }
    return events;
  }
}

/**
 * Whether the app is in touch with the server: `connected` while the event stream is open, `disconnected` while it is
 * waiting to connect again, and `not-paired` once the server has refused the key, after which it stops trying.
 */
export type ConnectionState = 'connecting' | 'connected' | 'disconnected' | 'not-paired';

export interface ConnectionListener {
  unit(unit: Unit): void;

  notification(notification: Notification): void;

  state(state: ConnectionState): void;
}

/** How long to wait before connecting again after each failure in a row; the last one repeats. */
export const RETRY_DELAYS_MS = [1_000, 2_000, 5_000, 10_000, 30_000];

/**
 * Keeps the event stream of the unit open, connecting again whenever it ends, and tells the listener every unit and
 * every change of the connection. The stream is read with `fetch` rather than `EventSource`, which cannot send the
 * key in a header.
 */
export class UnitConnection {

  private failures = 0;
  private stopped = false;
  private abort: AbortController | null = null;
  private retry: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly key: string,
              private readonly url: string,
              private readonly listener: ConnectionListener,
              private readonly fetchFn: typeof fetch = (input, init) => fetch(input, init)) {
  }

  start() {
    void this.connect();
  }

  /** Connects again right away, if it is waiting to, as when the device comes back online. */
  reconnectNow() {
    if (this.retry !== null) {
      clearTimeout(this.retry);
      this.retry = null;
      void this.connect();
    }
  }

  stop() {
    this.stopped = true;
    this.abort?.abort();
    if (this.retry !== null) {
      clearTimeout(this.retry);
    }
  }

  private async connect() {
    this.retry = null;
    this.abort = new AbortController();
    this.listener.state('connecting');
    try {
      const response = await this.fetchFn(this.url, {
        headers: { ...authorization(this.key), Accept: 'text/event-stream' },
        cache: 'no-store',
        signal: this.abort.signal,
      });
      if (response.status === 401) {
        this.stopped = true;
        this.listener.state('not-paired');
        return;
      }
      if (!response.ok || response.body === null) {
        throw new Error(`The event stream answered ${response.status}`);
      }
      await this.read(response.body);
    } catch {
      // The server is unreachable, or the stream broke; connect again below
    }
    if (!this.stopped) {
      this.listener.state('disconnected');
      const delay = RETRY_DELAYS_MS[Math.min(this.failures, RETRY_DELAYS_MS.length - 1)];
      this.failures++;
      this.retry = setTimeout(() => void this.connect(), delay);
    }
  }

  private async read(body: ReadableStream<Uint8Array>) {
    const parser = new EventParser();
    const decoder = new TextDecoder();
    const reader = body.getReader();
    let connected = false;
    while (true) {
      const { done, value } = await reader.read();
      if (done) {
        return;
      }
      for (const event of parser.push(decoder.decode(value, { stream: true }))) {
        if (event.event === 'unit') {
          if (!connected) {
            // Connected once the server has said something, not merely answered
            connected = true;
            this.failures = 0;
            this.listener.state('connected');
          }
          this.listener.unit(JSON.parse(event.data) as Unit);
        } else if (event.event === 'notification' && connected) {
          this.listener.notification(JSON.parse(event.data) as Notification);
        }
      }
    }
  }
}
