import 'fake-indexeddb/auto';
import { IDBFactory } from 'fake-indexeddb';
import { beforeEach, describe, expect, it } from 'vitest';
import type { Alarm } from './alarm';
import { Duty, type DutyStore, indexedDbDutyStore } from './duty';
import type { ScreenLock } from './wake-lock';

class FakeAlarm implements Alarm {
  unlocked = false;
  sounding = false;

  async unlock() {
    this.unlocked = true;
  }

  start() {
    // Like a browser, it plays only once a tap has unlocked it
    this.sounding = this.unlocked;
  }

  stop() {
    this.sounding = false;
  }
}

class FakeScreen implements ScreenLock {
  held = false;
  acquired = 0;

  constructor(private readonly supported = true) {
  }

  async acquire() {
    this.acquired++;
    this.held = this.supported;
    return this.supported;
  }

  async release() {
    this.held = false;
  }
}

describe('the on-call duty', () => {
  let alarm: FakeAlarm;
  let screen: FakeScreen;
  let store: DutyStore;
  let duty: Duty;

  beforeEach(() => {
    alarm = new FakeAlarm();
    screen = new FakeScreen();
    store = indexedDbDutyStore(new IDBFactory());
    duty = new Duty(alarm, screen, store, () => undefined);
  });

  it('makes no sound for an alert before it has started', async () => {
    duty.alert();

    expect(alarm.sounding).toBe(false);
    expect(duty.state).toBe('off');
  });

  it('sounds the alarm for an alert once started, until the alert is acknowledged', async () => {
    await duty.start();

    duty.alert();
    expect(alarm.sounding).toBe(true);

    duty.acknowledge();
    expect(alarm.sounding).toBe(false);
  });

  it('keeps the screen on, and takes the lock again when the app becomes visible', async () => {
    await duty.start();
    expect(screen.held).toBe(true);
    expect(duty.screenKeptOn).toBe(true);

    screen.held = false;
    await duty.visible();

    expect(screen.held).toBe(true);
  });

  it('tells when the device cannot keep the screen on', async () => {
    duty = new Duty(alarm, new FakeScreen(false), store, () => undefined);

    await duty.start();

    expect(duty.state).toBe('on');
    expect(duty.screenKeptOn).toBe(false);
  });

  it('stops the alarm and lets the screen go when it ends', async () => {
    await duty.start();
    duty.alert();

    await duty.end();

    expect(alarm.sounding).toBe(false);
    expect(screen.held).toBe(false);
    duty.alert();
    expect(alarm.sounding).toBe(false);
  });

  it('does not take the screen lock when the app becomes visible off duty', async () => {
    await duty.visible();

    expect(screen.acquired).toBe(0);
  });

  it('is interrupted after a reload during it, and silent until resumed with a tap', async () => {
    await duty.start();
    const reloaded = new Duty(new FakeAlarm(), new FakeScreen(), store, () => undefined);

    await reloaded.restore();
    expect(reloaded.state).toBe('interrupted');

    await reloaded.start();
    expect(reloaded.state).toBe('on');
  });

  it('is off after a reload once it was ended', async () => {
    await duty.start();
    await duty.end();
    const reloaded = new Duty(new FakeAlarm(), new FakeScreen(), store, () => undefined);

    await reloaded.restore();

    expect(reloaded.state).toBe('off');
  });
});
