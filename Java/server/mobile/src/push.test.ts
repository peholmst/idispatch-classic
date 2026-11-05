import { describe, expect, it, vi } from 'vitest';
import type { Notification } from './api';
import { type AlertMessage, isAlertMessage, receivePush, type PushContext, VIBRATION } from './push';

const KEY = 'k'.repeat(43);
const ALERT: Notification = {
  id: 7, assignmentId: 3, timestamp: '2026-11-04T12:00:00Z', urgency: 'A', typeCode: '402', type: 'Rakennuspalo',
  municipality: 'Parainen', address: 'Rantatie 1', description: 'Savua katolla', resources: ['RVS911', 'RVS913'],
};

function context(options: { isNew?: boolean; windows?: boolean[]; key?: string | null } = {}) {
  const posted: AlertMessage[] = [];
  const fake = {
    store: vi.fn(async () => options.isNew ?? true),
    key: vi.fn(async () => options.key === undefined ? KEY : options.key),
    confirm: vi.fn(async () => true),
    windows: vi.fn(async () => (options.windows ?? []).map(visible => ({
      visible, postMessage: (message: AlertMessage) => posted.push(message),
    }))),
    showNotification: vi.fn(async () => undefined),
  } satisfies PushContext;
  return { fake, posted };
}

describe('an alert pushed while the app is closed', () => {
  it('is stored, so that it is in the message view when the app opens', async () => {
    const { fake } = context();

    await receivePush({ locale: 'fi', notification: ALERT }, fake);

    expect(fake.store).toHaveBeenCalledWith(ALERT);
  });

  it('is confirmed to the server, which stores a receipt even if the app is never opened', async () => {
    const { fake } = context();

    await receivePush({ locale: 'fi', notification: ALERT }, fake);

    expect(fake.confirm).toHaveBeenCalledWith(KEY, 7);
  });

  it('shows a notification in the language of the destination, that vibrates and stays until tapped', async () => {
    const { fake } = context();

    await receivePush({ locale: 'sv', notification: ALERT }, fake);

    const [title, options] = fake.showNotification.mock.calls[0] as unknown as [string, NotificationOptions & { vibrate: number[] }];
    expect(title).toBe('Nytt larm: A 402 Rakennuspalo');
    expect(options.body).toContain('Rantatie 1, Parainen');
    expect(options.body).toContain('Savua katolla');
    expect(options.body).toContain('Enheter: RVS911, RVS913');
    expect(options.requireInteraction).toBe(true);
    expect(options.vibrate).toEqual(VIBRATION);
    expect(options.tag).toBe('alert-7');
  });

  it('is not confirmed by an app that is not paired', async () => {
    const { fake } = context({ key: null });

    await receivePush({ locale: 'fi', notification: ALERT }, fake);

    expect(fake.confirm).not.toHaveBeenCalled();
  });
});

describe('an alert pushed while the app is open', () => {
  it('is told to the app, which shows it itself when it is visible', async () => {
    const { fake, posted } = context({ windows: [true] });

    await receivePush({ locale: 'fi', notification: ALERT }, fake);

    expect(posted).toEqual([{ type: 'alert', notification: ALERT, isNew: true }]);
    expect(fake.showNotification).not.toHaveBeenCalled();
  });

  it('is not new to the app if it came over the event stream first', async () => {
    const { fake, posted } = context({ isNew: false, windows: [true] });

    await receivePush({ locale: 'fi', notification: ALERT }, fake);

    expect(posted[0].isNew).toBe(false);
    expect(fake.confirm).toHaveBeenCalledWith(KEY, 7);
  });

  it('shows a notification when the app is in the background, even if it got the alert already', async () => {
    const { fake } = context({ isNew: false, windows: [false] });

    await receivePush({ locale: 'fi', notification: ALERT }, fake);

    expect(fake.showNotification).toHaveBeenCalledOnce();
  });
});

describe('a message from the service worker', () => {
  it('is an alert only when it says so', () => {
    expect(isAlertMessage({ type: 'alert', notification: ALERT, isNew: true })).toBe(true);
    expect(isAlertMessage({ type: 'other' })).toBe(false);
    expect(isAlertMessage(null)).toBe(false);
  });
});
