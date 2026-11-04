import { beforeEach, describe, expect, it, vi } from 'vitest';
import { type ServiceWorkerRegistrar, Updates } from './updates';

class FakeRegistrar implements ServiceWorkerRegistrar {
  private listener: ((event: { isUpdate?: boolean }) => void) | null = null;
  registered = false;

  async register() {
    this.registered = true;
  }

  async update() {
  }

  addEventListener(_type: 'controlling', listener: (event: { isUpdate?: boolean }) => void) {
    this.listener = listener;
  }

  takeOver(isUpdate: boolean) {
    this.listener?.({ isUpdate });
  }
}

describe('a new version of the app', () => {
  let registrar: FakeRegistrar;
  let reload: ReturnType<typeof vi.fn<() => void>>;

  beforeEach(() => {
    registrar = new FakeRegistrar();
    reload = vi.fn<() => void>();
  });

  function updates(mayReload: () => boolean) {
    const updates = new Updates(mayReload, () => undefined, reload, () => registrar);
    updates.register();
    return updates;
  }

  it('is reloaded into at once off duty', () => {
    updates(() => true);

    registrar.takeOver(true);

    expect(registrar.registered).toBe(true);
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('waits during the duty, and is reloaded into once it ends', () => {
    let onDuty = true;
    const app = updates(() => !onDuty);

    registrar.takeOver(true);
    expect(app.waiting).toBe(true);
    expect(reload).not.toHaveBeenCalled();

    onDuty = false;
    app.reloadIfWaiting();
    expect(reload).toHaveBeenCalledTimes(1);
    app.reloadIfWaiting();
    expect(reload).toHaveBeenCalledTimes(1);
  });

  it('is not what the first service worker taking over the page is', () => {
    updates(() => true);

    registrar.takeOver(false);

    expect(reload).not.toHaveBeenCalled();
  });

  it('is not reloaded into when none is waiting', () => {
    updates(() => true).reloadIfWaiting();

    expect(reload).not.toHaveBeenCalled();
  });
});
