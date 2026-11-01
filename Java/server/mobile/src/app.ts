import { html, LitElement, nothing } from 'lit';
import '@vaadin/button';
import { apiBase, changeStatus, confirmNotification, type Notification, type Unit } from './api';
import { type ConnectionState, UnitConnection } from './events';
import { formatTime } from './format';
import { MessageStore } from './messages';
import { indexedDbKeyStore, pair } from './pairing';
import { type Texts, textsFor } from './texts';

/**
 * The app: pairs with the key in the address, if there is one, keeps the unit's event stream open, and shows the
 * unit's status with a button for each state it may set next, and the alerts it has received. The buttons work only
 * while the stream is open, that is while the server is reachable: a change is never queued to be sent later. An alert
 * is stored before the app confirms it to the server, and stays until the user deletes it.
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

  private readonly store = new MessageStore();

  private key: string | null = null;
  private stream: UnitConnection | null = null;
  private readonly reconnect = () => this.stream?.reconnectNow();
  private readonly reconnectWhenVisible = () => {
    if (document.visibilityState === 'visible') {
      this.reconnect();
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
  }

  // Rendered without shadow DOM, so that the Aura theme's document styles reach the Vaadin components
  protected createRenderRoot() {
    return this;
  }

  async connectedCallback() {
    super.connectedCallback();
    window.addEventListener('online', this.reconnect);
    document.addEventListener('visibilitychange', this.reconnectWhenVisible);
    this.key = await pair(window.location, url => window.history.replaceState(null, '', url), indexedDbKeyStore());
    this.paired = this.key !== null;
    await this.store.forgetOldDeleted();
    this.messages = await this.store.list();
    if (this.key !== null) {
      this.stream = new UnitConnection(this.key, apiBase() + 'events', {
        unit: unit => {
          this.unit = unit;
          document.documentElement.lang = unit.locale;
        },
        notification: notification => void this.receive(notification),
        state: state => {
          this.connection = state;
          if (state === 'not-paired') {
            this.paired = false;
          }
        },
      });
      this.stream.start();
    }
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener('online', this.reconnect);
    document.removeEventListener('visibilitychange', this.reconnectWhenVisible);
    this.stream?.stop();
  }

  /**
   * Stores the alert, then confirms it to the server. An alert that comes again, because the confirmation was lost, is
   * confirmed again but not stored twice, nor brought back after the user deleted it.
   */
  private async receive(notification: Notification) {
    if (await this.store.add(notification)) {
      this.messages = await this.store.list();
    }
    if (this.key !== null) {
      await confirmNotification(this.key, notification.id);
    }
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
      <header class="unit">
        <h1 class="call-sign">${unit.callSign}</h1>
        <p class="status ${stateClass(unit.status?.state)}" data-state=${unit.status?.state ?? ''}>
          ${unit.status?.label ?? t.noStatus}
        </p>
      </header>
      ${online ? nothing : html`<p class="banner" role="status">${t.connectionLost}</p>`}
      ${this.error === null ? nothing : html`<p class="error" role="alert">${t[this.error]}</p>`}
      <section class="status-panel" aria-label=${t.changeStatus}>
        ${unit.nextStates.map(next => html`
          <vaadin-button class="status-button ${stateClass(next.state)}" data-state=${next.state} ?disabled=${!online || this.changing}
                         @click=${() => this.change(next.state)}>${next.label}</vaadin-button>`)}
      </section>
      <section class="messages">
        <h2>${t.messages}</h2>
        ${this.messages.length === 0
          ? html`<p class="no-messages">${t.noMessages}</p>`
          : this.messages.map(message => this.renderMessage(message, t, unit.locale))}
      </section>`;
  }

  private renderMessage(message: Notification, t: Texts, locale: string) {
    return html`
      <article class="message-card" data-id=${message.id}>
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
