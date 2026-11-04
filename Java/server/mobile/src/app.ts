import { html, LitElement, nothing, svg } from 'lit';
import '@vaadin/button';
import '@vaadin/dialog';
import '@vaadin/icon';
import type { Icon } from '@vaadin/icon';
import '@vaadin/menu-bar';
import type { MenuBarItem, MenuBarItemSelectedEvent } from '@vaadin/menu-bar';
import { WebAudioAlarm } from './alarm';
import { apiBase, changeStatus, confirmNotification, type Notification, type Unit } from './api';
import { type ConnectionState, UnitConnection } from './events';
import { alertState } from './alerts';
import { Duty, indexedDbDutyStore } from './duty';
import { formatTime } from './format';
import { MessageStore } from './messages';
import { indexedDbKeyStore, pair } from './pairing';
import { isAlertMessage } from './push';
import { PushSetup, type PushState } from './push-subscription';
import { UnitCache } from './unit-cache';
import { type Texts, textsFor } from './texts';
import { Updates } from './updates';
import { WakeLockScreenLock } from './wake-lock';

/**
 * Vaadin Icons' `ellipsis-dots-v` (Apache License 2.0), on its own, so that the app doesn't load the whole icon set,
 * which is larger than the app, for one icon.
 */
const MORE_ICON = svg`<svg viewBox="0 0 16 16"><path d="M10 2c0 1.105-0.895 2-2 2s-2-0.895-2-2c0-1.105 0.895-2 2-2s2 0.895 2 2z"></path><path d="M10 8c0 1.105-0.895 2-2 2s-2-0.895-2-2c0-1.105 0.895-2 2-2s2 0.895 2 2z"></path><path d="M10 14c0 1.105-0.895 2-2 2s-2-0.895-2-2c0-1.105 0.895-2 2-2s2 0.895 2 2z"></path></svg>`;

/**
 * The app: pairs with the key in the address, if there is one, keeps the unit's event stream open, and shows the
 * alerts the unit has received. The unit's status is always in a corner of the screen, and opens a dialog with a
 * button for each state it may set next. The buttons work only while the stream is open, that is while the server is
 * reachable: a change is never queued to be sent later. An alert is stored before the app confirms it to the server,
 * and stays until the user deletes it.
 * <p>
 * A new alert takes over the screen until someone acknowledges it. While the unit is on call (see {@link Duty}), it
 * also sounds the alarm and the screen is kept on.
 * <p>
 * Once the user lets the app show notifications, the alerts also come by Web Push, which the service worker receives
 * while the app is in the background or closed (`push.ts`): it stores and confirms each one, and tells the app about
 * it, which takes an alert that it stored first as new.
 */
export class MobileApp extends LitElement {

  static properties = {
    paired: { state: true },
    connection: { state: true },
    unit: { state: true },
    changing: { state: true },
    error: { state: true },
    messages: { state: true },
    deleting: { state: true },
    statusOpen: { state: true },
    newAlert: { state: true },
    push: { state: true },
  };

  declare paired: boolean | null;
  declare connection: ConnectionState;
  declare unit: Unit | null;
  /** Whether a status change is on its way, during which the buttons are disabled. */
  declare changing: boolean;
  declare error: keyof Texts | null;
  /** The stored alerts, newest first. */
  declare messages: Notification[];
  /** The alert whose deletion the user is asked to confirm. */
  declare deleting: number | null;
  /** Whether the dialog for changing the status is open. */
  declare statusOpen: boolean;
  /** The alert that has taken over the screen until someone acknowledges it. */
  declare newAlert: Notification | null;
  /** Whether the alerts come by push when the app is closed; `null` until the app knows. */
  declare push: PushState | null;

  private readonly duty = new Duty(new WebAudioAlarm(), new WakeLockScreenLock(), indexedDbDutyStore(),
    () => this.dutyChanged());
  private readonly updates = new Updates(() => this.duty.state === 'off', () => this.requestUpdate());

  private readonly store = new MessageStore();
  private readonly unitCache = new UnitCache();

  private key: string | null = null;
  private stream: UnitConnection | null = null;
  private pushSetup: PushSetup | null = null;
  /** An alert that the service worker received by push. */
  private readonly receivedByPush = (event: MessageEvent) => {
    if (isAlertMessage(event.data)) {
      void this.arrived(event.data.notification, event.data.isNew);
    }
  };
  private readonly reconnect = () => this.stream?.reconnectNow();
  private readonly reconnectWhenVisible = () => {
    if (document.visibilityState === 'visible') {
      this.reconnect();
      void this.duty.visible();
    }
  };

  constructor() {
    super();
    this.paired = null;
    this.connection = 'connecting';
    this.unit = null;
    this.changing = false;
    this.error = null;
    this.messages = [];
    this.deleting = null;
    this.statusOpen = false;
    this.newAlert = null;
    this.push = null;
  }

  // Rendered without shadow DOM, so that the Aura theme's document styles reach the Vaadin components
  protected createRenderRoot() {
    return this;
  }

  async connectedCallback() {
    super.connectedCallback();
    window.addEventListener('online', this.reconnect);
    document.addEventListener('visibilitychange', this.reconnectWhenVisible);
    navigator.serviceWorker?.addEventListener('message', this.receivedByPush);
    this.updates.register();
    await this.duty.restore();
    this.key = await pair(window.location, url => window.history.replaceState(null, '', url), indexedDbKeyStore());
    this.paired = this.key !== null;
    await this.store.forgetOldDeleted();
    this.messages = await this.store.list();
    if (this.key !== null) {
      // The last known unit, shown until the server answers, also when it can't be reached
      this.unit = await this.unitCache.get();
      if (this.unit !== null) {
        document.documentElement.lang = this.unit.locale;
      }
      this.stream = new UnitConnection(this.key, apiBase() + 'events', {
        unit: unit => {
          this.unit = unit;
          document.documentElement.lang = unit.locale;
          void this.unitCache.set(unit);
        },
        notification: notification => void this.receive(notification),
        state: state => {
          this.connection = state;
          if (state === 'not-paired') {
            this.paired = false;
          } else if (state === 'connected' && this.push === 'failed') {
            // Telling the server failed, as when it couldn't be reached; it can now
            void this.refreshPush();
          }
        },
      });
      this.stream.start();
      this.pushSetup = new PushSetup(this.key);
      void this.refreshPush();
    }
  }

  private async refreshPush() {
    this.push = await this.pushSetup?.refresh() ?? null;
  }

  private async enablePush() {
    this.push = await this.pushSetup?.enable() ?? null;
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener('online', this.reconnect);
    document.removeEventListener('visibilitychange', this.reconnectWhenVisible);
    navigator.serviceWorker?.removeEventListener('message', this.receivedByPush);
    this.stream?.stop();
  }

  /**
   * Stores the alert, then confirms it to the server. An alert that comes again, because the confirmation was lost, is
   * confirmed again but not stored twice, nor brought back after the user deleted it. A new alert takes over the
   * screen, replacing one shown before it, and sounds the alarm while the unit is on call.
   */
  private async receive(notification: Notification) {
    await this.arrived(notification, await this.store.add(notification));
    if (this.key !== null) {
      await confirmNotification(this.key, notification.id);
    }
  }

  /**
   * Shows an alert just stored, by the app or by its service worker, if it was new. One that came both over the event
   * stream and by push is new only to whichever stored it first.
   */
  private async arrived(notification: Notification, isNew: boolean) {
    if (!isNew) {
      return;
    }
    this.messages = await this.store.list();
    if (this.newAlert === null || notification.timestamp >= this.newAlert.timestamp) {
      this.newAlert = notification;
    }
    this.duty.alert();
  }

  /** Silences the alarm and lets the new alert go back to the list. */
  private acknowledge() {
    this.duty.acknowledge();
    this.newAlert = null;
    // The new alert is at the top of the list
    this.querySelector('.message-list')?.scrollTo({ top: 0 });
  }

  private dutyChanged() {
    this.requestUpdate();
    // An update waits while the unit is on call; once it is off, the app may reload into it
    this.updates.reloadIfWaiting();
  }

  private async deleteMessage(id: number) {
    this.deleting = null;
    await this.store.delete(id);
    this.messages = await this.store.list();
  }

  private async change(state: string) {
    if (this.key === null) {
      return;
    }
    this.changing = true;
    this.error = null;
    const result = await changeStatus(this.key, state);
    this.changing = false;
    switch (result.kind) {
      case 'changed':
        this.unit = result.unit;
        this.statusOpen = false;
        break;
      case 'refused':
        this.unit = result.unit;
        this.error = 'statusRefused';
        break;
      case 'not-paired':
        this.paired = false;
        break;
      case 'no-connection':
        this.error = 'statusFailed';
        break;
    }
  }

  render() {
    const t = textsFor(this.unit?.locale ?? navigator.language);
    if (this.paired === null) {
      return html`<p class="message">${t.loading}</p>`;
    }
    if (!this.paired) {
      return html`
        <section class="message">
          <h1>${t.notPaired}</h1>
          <p>${t.notPairedHelp}</p>
        </section>`;
    }
    if (this.unit === null) {
      return this.connection === 'disconnected'
        ? html`
          <section class="message">
            <h1>${t.noConnection}</h1>
            <vaadin-button theme="primary" @click=${this.reconnect}>${t.retry}</vaadin-button>
          </section>`
        : html`<p class="message">${t.loading}</p>`;
    }
    const online = this.connection === 'connected';
    const { unit } = this;
    return html`
      <header class="top-bar">
        <h1 class="call-sign">${unit.callSign}</h1>
        ${this.renderDutyButton(t)}
      </header>
      ${online ? nothing : html`<p class="banner" role="status">${t.connectionLost}</p>`}
      ${this.renderDutyNote(t)}
      ${this.renderPushNote(t)}
      <section class="messages">
        <h2>${t.messages}</h2>
        <div class="message-list">
          ${this.messages.length === 0
            ? html`<p class="no-messages">${t.noMessages}</p>`
            : this.messages.map(message => this.renderMessage(message, t, unit.locale))}
        </div>
      </section>
      <vaadin-button class="status-corner ${stateClass(unit.status?.state)}" data-state=${unit.status?.state ?? ''}
                     aria-label=${`${t.changeStatus}: ${unit.status?.label ?? t.noStatus}`}
                     @click=${() => (this.statusOpen = true)}>
        ${unit.status?.label ?? t.noStatus}
      </vaadin-button>
      ${this.renderStatusDialog(unit, t, online)}
      ${this.renderNewAlert(t, unit.locale)}
      ${this.renderResumeDuty(t)}`;
  }

  /**
   * Starts the duty, or, while it is on, says so with a menu that ends it, so that ending it takes two taps and the top
   * bar stays on one line on a phone.
   */
  private renderDutyButton(t: Texts) {
    return this.duty.state === 'on'
      ? html`
        <span class="duty-on">${t.onDuty}</span>
        <vaadin-menu-bar class="duty-menu" theme="tertiary icon" .items=${this.dutyMenu(t)}
                         @item-selected=${(event: MenuBarItemSelectedEvent) => {
                           if (event.detail.value.text === t.endDuty) {
                             void this.duty.end();
                           }
                         }}></vaadin-menu-bar>`
      : html`<vaadin-button theme="primary" @click=${() => this.duty.start()}>${t.startDuty}</vaadin-button>`;
  }

  private dutyMenuItems: { language: string; items: MenuBarItem[] } | null = null;

  /** The duty menu's items, made once per language, as the menu bar renders itself again whenever they change. */
  private dutyMenu(t: Texts): MenuBarItem[] {
    if (this.dutyMenuItems?.language !== t.endDuty) {
      const icon = document.createElement('vaadin-icon') as Icon;
      icon.svg = MORE_ICON;
      // The button's name for screen readers, which an icon gives only with a role
      icon.setAttribute('role', 'img');
      icon.setAttribute('aria-label', t.menu);
      this.dutyMenuItems = { language: t.endDuty, items: [{ component: icon, children: [{ text: t.endDuty }] }] };
    }
    return this.dutyMenuItems.items;
  }

  private renderDutyNote(t: Texts) {
    if (this.duty.state === 'off') {
      return html`<p class="duty-note" role="status">${t.dutyOff}</p>`;
    }
    if (this.duty.state === 'on') {
      return html`
        ${this.duty.screenKeptOn ? nothing : html`<p class="duty-note" role="status">${t.screenMayTurnOff}</p>`}
        ${this.updates.waiting ? html`<p class="duty-note" role="status">${t.updateWaiting}</p>` : nothing}`;
    }
    return nothing;
  }

  /** Asks for notifications, or tells that alerts come only while the app is open. */
  private renderPushNote(t: Texts) {
    switch (this.push) {
      case 'ask':
        return html`
          <div class="duty-note push-note" role="status">
            <span>${t.pushAsk}</span>
            <vaadin-button theme="primary" @click=${() => this.enablePush()}>${t.enablePush}</vaadin-button>
          </div>`;
      case 'denied':
        return html`<p class="duty-note" role="status">${t.pushDenied}</p>`;
      case 'failed':
        return html`<p class="duty-note" role="status">${t.pushFailed}</p>`;
      default:
        return nothing;
    }
  }

  /** The states the unit may set next, as large buttons in the dispatcher's colors. */
  private renderStatusDialog(unit: Unit, t: Texts, online: boolean) {
    return html`
      <vaadin-dialog class="status-dialog" header-title=${t.changeStatus} .opened=${this.statusOpen}
                     @opened-changed=${(event: CustomEvent<{ value: boolean }>) => {
                       this.statusOpen = event.detail.value;
                       if (!event.detail.value) {
                         this.error = null;
                       }
                     }}>
        <p class="status-label">${t.currentStatus}</p>
        <p class="status ${stateClass(unit.status?.state)}" data-state=${unit.status?.state ?? ''}>
          ${unit.status?.label ?? t.noStatus}
        </p>
        ${online ? nothing : html`<p class="banner" role="status">${t.connectionLost}</p>`}
        ${this.error === null ? nothing : html`<p class="error" role="alert">${t[this.error]}</p>`}
        ${unit.nextStates.length === 0 ? nothing : html`<p class="status-label">${t.changeTo}</p>`}
        <div class="status-panel">
          ${unit.nextStates.map(next => html`
            <vaadin-button class="status-button ${stateClass(next.state)}" data-state=${next.state}
                           ?disabled=${!online || this.changing}
                           @click=${() => this.change(next.state)}>${next.label}</vaadin-button>`)}
        </div>
        <div slot="footer" class="dialog-footer">
          <vaadin-button @click=${() => (this.statusOpen = false)}>${t.close}</vaadin-button>
        </div>
      </vaadin-dialog>`;
  }

  /** A new alert, over everything else, until someone acknowledges it. */
  private renderNewAlert(t: Texts, locale: string) {
    const alert = this.newAlert;
    return html`
      <vaadin-dialog class="new-alert" header-title=${t.newAlert} .opened=${alert !== null} no-close-on-outside-click
                     no-close-on-esc>
        ${alert === null ? nothing : html`
          <p class="new-alert-type"><span class="urgency">${alert.urgency}</span> ${alert.typeCode} ${alert.type}</p>
          <p class="new-alert-address">${alert.address}</p>
          <p class="new-alert-municipality">${alert.municipality}</p>
          ${alert.description ? html`<p class="description">${alert.description}</p>` : nothing}
          <p class="resources">${t.resources}: ${alert.resources.join(', ')}</p>
          <p class="resources"><time datetime=${alert.timestamp}>${formatTime(alert.timestamp, locale)}</time></p>`}
        <div slot="footer" class="dialog-footer">
          <vaadin-button theme="primary" class="acknowledge" @click=${() => this.acknowledge()}>
            ${t.acknowledge}
          </vaadin-button>
        </div>
      </vaadin-dialog>`;
  }

  /**
   * After the app was reloaded during the duty, a tap resumes it: browsers play sound only after one, so the alarm
   * would otherwise stay silent without anyone noticing.
   */
  private renderResumeDuty(t: Texts) {
    return html`
      <vaadin-dialog class="resume-duty" header-title=${t.dutyInterrupted}
                     .opened=${this.duty.state === 'interrupted' && this.newAlert === null}
                     no-close-on-outside-click no-close-on-esc>
        <p>${t.dutyInterruptedText}</p>
        <div slot="footer" class="dialog-footer">
          <vaadin-button @click=${() => this.duty.end()}>${t.endDuty}</vaadin-button>
          <vaadin-button theme="primary" @click=${() => this.duty.start()}>${t.resumeDuty}</vaadin-button>
        </div>
      </vaadin-dialog>`;
  }

  private renderMessage(message: Notification, t: Texts, locale: string) {
    const state = alertState(message, this.messages, this.unit);
    return html`
      <article class="message-card ${state}" data-id=${message.id}>
        ${state === 'current' ? nothing : html`<p class="alert-state">${t[state]}</p>`}
        <header>
          <span class="urgency">${message.urgency}</span>
          <span class="type">${message.typeCode} ${message.type}</span>
          <time datetime=${message.timestamp}>${formatTime(message.timestamp, locale)}</time>
        </header>
        <p class="address">${message.address}, ${message.municipality}</p>
        ${message.description ? html`<p class="description">${message.description}</p>` : nothing}
        <p class="resources">${t.resources}: ${message.resources.join(', ')}</p>
        ${this.deleting === message.id
          ? html`
            <div class="confirm-delete" role="group" aria-label=${t.confirmDelete}>
              <span>${t.confirmDelete}</span>
              <vaadin-button theme="error primary" @click=${() => this.deleteMessage(message.id)}>
                ${t.delete}
              </vaadin-button>
              <vaadin-button @click=${() => (this.deleting = null)}>${t.cancel}</vaadin-button>
            </div>`
          : html`
            <vaadin-button theme="tertiary" class="delete" @click=${() => (this.deleting = message.id)}>
              ${t.delete}
            </vaadin-button>`}
      </article>`;
  }
}

/** The class that colors a state as the dispatcher's views do (the server's `ResourceFormatters`). */
function stateClass(state: string | undefined) {
  return state === undefined ? '' : `resource-state-${state.toLowerCase()}`;
}

customElements.define('idispatch-mobile-app', MobileApp);
