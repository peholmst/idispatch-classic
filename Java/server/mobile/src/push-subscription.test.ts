import { describe, expect, it, vi } from 'vitest';
import { base64UrlToBytes, type PushBrowser, PushSetup } from './push-subscription';

const KEY = 'k'.repeat(43);
const SERVER_KEY = 'BMqJtpGzQVU3IkBBkjYR6ph-Ef6c_z0Ivdhvp21wQSJd2-YLW4SWghn-GSebt5a_0t9M7dKd1zU2fJy8oWobvqE';
const OTHER_KEY = 'BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4';

function subscription(serverKey: string, endpoint = 'https://push.example.com/1') {
  return {
    options: { applicationServerKey: base64UrlToBytes(serverKey).buffer },
    unsubscribe: vi.fn(async () => true),
    toJSON: () => ({ endpoint, keys: { p256dh: 'p', auth: 'a' } }),
  } as unknown as PushSubscription & { unsubscribe: ReturnType<typeof vi.fn> };
}

function setUp(options: { permission?: NotificationPermission; granted?: NotificationPermission;
  existing?: PushSubscription | null; serverKey?: string | null; registered?: boolean; noPush?: boolean } = {}) {
  let permission = options.permission ?? 'granted';
  const manager = {
    getSubscription: vi.fn(async () => options.existing ?? null),
    subscribe: vi.fn(async (_init: PushSubscriptionOptionsInit) => subscription(SERVER_KEY, 'https://push.example.com/new')),
  };
  const browser: PushBrowser = {
    permission: () => permission,
    requestPermission: vi.fn(async () => (permission = options.granted ?? 'granted')),
    pushManager: async () => options.noPush ? null : manager as unknown as PushManager,
  };
  const api = {
    pushKey: vi.fn(async () => options.serverKey === undefined ? SERVER_KEY : options.serverKey),
    register: vi.fn(async () => options.registered ?? true),
  };
  return { setup: new PushSetup(KEY, browser, api), manager, browser, api };
}

describe('subscribing to push', () => {
  it('gives the server the subscription once the user has allowed notifications', async () => {
    const { setup, manager, api } = setUp();

    expect(await setup.refresh()).toBe('on');

    const init = manager.subscribe.mock.calls[0][0];
    expect(init.userVisibleOnly).toBe(true);
    expect(new Uint8Array(init.applicationServerKey as ArrayBuffer)).toEqual(base64UrlToBytes(SERVER_KEY));
    expect(api.register).toHaveBeenCalledWith(KEY, expect.objectContaining({ endpoint: 'https://push.example.com/new' }));
  });

  it('asks the user first, and only after a tap', async () => {
    const { setup, browser, manager } = setUp({ permission: 'default' });

    expect(await setup.refresh()).toBe('ask');
    expect(manager.subscribe).not.toHaveBeenCalled();

    expect(await setup.enable()).toBe('on');
    expect(browser.requestPermission).toHaveBeenCalledOnce();
  });

  it('does nothing once the user has said no', async () => {
    const { setup, manager } = setUp({ permission: 'default', granted: 'denied' });

    expect(await setup.enable()).toBe('denied');
    expect(manager.subscribe).not.toHaveBeenCalled();
  });

  it('keeps the subscription it has, and gives it to the server again, as after pairing again', async () => {
    const existing = subscription(SERVER_KEY);
    const { setup, manager, api } = setUp({ existing });

    expect(await setup.refresh()).toBe('on');

    expect(manager.subscribe).not.toHaveBeenCalled();
    expect(api.register).toHaveBeenCalledWith(KEY, expect.objectContaining({ endpoint: 'https://push.example.com/1' }));
  });

  it('replaces a subscription made with another server key', async () => {
    const existing = subscription(OTHER_KEY);
    const { setup, manager } = setUp({ existing });

    expect(await setup.refresh()).toBe('on');

    expect(existing.unsubscribe).toHaveBeenCalled();
    expect(manager.subscribe).toHaveBeenCalled();
  });

  it('is not available when the server sends no pushes, or the browser has no Web Push', async () => {
    expect(await setUp({ serverKey: null }).setup.refresh()).toBe('unavailable');
    expect(await setUp({ noPush: true }).setup.refresh()).toBe('unavailable');
  });

  it('fails, to be tried again, when the server does not take the subscription', async () => {
    expect(await setUp({ registered: false }).setup.refresh()).toBe('failed');
  });
});
