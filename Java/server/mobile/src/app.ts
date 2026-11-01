import { html, LitElement } from 'lit';
import '@vaadin/button';
import { fetchUnit, type Unit } from './api';
import { indexedDbKeyStore, pair } from './pairing';
import { textsFor } from './texts';

type View =
  | { kind: 'loading' }
  | { kind: 'not-paired' }
  | { kind: 'no-connection' }
  | { kind: 'unit'; unit: Unit };

/**
 * The app: pairs with the key in the address, if there is one, and shows the unit it is paired with.
 */
export class MobileApp extends LitElement {

  static properties = {
    view: { state: true },
  };

  declare view: View;
  private key: string | null = null;

  constructor() {
    super();
    this.view = { kind: 'loading' };
  }

  // Rendered without shadow DOM, so that the Aura theme's document styles reach the Vaadin components
  protected createRenderRoot() {
    return this;
  }

  async connectedCallback() {
    super.connectedCallback();
    this.key = await pair(window.location, url => window.history.replaceState(null, '', url), indexedDbKeyStore());
    await this.load();
  }

  private async load() {
    if (this.key === null) {
      this.view = { kind: 'not-paired' };
      return;
    }
    this.view = { kind: 'loading' };
    const result = await fetchUnit(this.key);
    this.view = result.kind === 'unit' ? { kind: 'unit', unit: result.unit } : { kind: result.kind };
    document.documentElement.lang = this.locale();
  }

  private locale() {
    return this.view.kind === 'unit' ? this.view.unit.locale : navigator.language;
  }

  render() {
    const t = textsFor(this.locale());
    switch (this.view.kind) {
      case 'loading':
        return html`<p class="message">${t.loading}</p>`;
      case 'not-paired':
        return html`
          <section class="message">
            <h1>${t.notPaired}</h1>
            <p>${t.notPairedHelp}</p>
          </section>`;
      case 'no-connection':
        return html`
          <section class="message">
            <h1>${t.noConnection}</h1>
            <vaadin-button theme="primary" @click=${() => this.load()}>${t.retry}</vaadin-button>
          </section>`;
      case 'unit': {
        const { unit } = this.view;
        return html`
          <header class="unit">
            <h1 class="call-sign">${unit.callSign}</h1>
            <p class="status" data-state=${unit.status?.state ?? ''}>${unit.status?.label ?? t.noStatus}</p>
          </header>`;
      }
    }
  }
}

customElements.define('idispatch-mobile-app', MobileApp);
