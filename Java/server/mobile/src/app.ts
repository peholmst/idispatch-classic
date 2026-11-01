import { html, LitElement, nothing } from 'lit';
import '@vaadin/button';
import { apiBase, changeStatus, type Unit } from './api';
import { type ConnectionState, UnitConnection } from './events';
import { indexedDbKeyStore, pair } from './pairing';
import { type Texts, textsFor } from './texts';

/**
 * The app: pairs with the key in the address, if there is one, keeps the unit's event stream open, and shows the
 * unit's status with a button for each state it may set next. The buttons work only while the stream is open, that is
 * while the server is reachable: a change is never queued to be sent later.
 */
export class MobileApp extends LitElement {

  static properties = {
    paired: { state: true },
    connection: { state: true },
    unit: { state: true },
    changing: { state: true },
    error: { state: true },
  };

  declare paired: boolean | null;
  declare connection: ConnectionState;
  declare unit: Unit | null;
  /** Whether a status change is on its way, during which the buttons are disabled. */
  declare changing: boolean;
  declare error: keyof Texts | null;

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
    if (this.key !== null) {
      this.stream = new UnitConnection(this.key, apiBase() + 'events', {
        unit: unit => {
          this.unit = unit;
          document.documentElement.lang = unit.locale;
        },
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
      </section>`;
  }
}

/** The class that colors a state as the dispatcher's views do (the server's `ResourceFormatters`). */
function stateClass(state: string | undefined) {
  return state === undefined ? '' : `resource-state-${state.toLowerCase()}`;
}

customElements.define('idispatch-mobile-app', MobileApp);
