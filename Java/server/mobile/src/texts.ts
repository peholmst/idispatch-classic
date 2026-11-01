/**
 * The app's texts, in the languages a destination can have. The app speaks the destination's language once it knows
 * it, and the device's language (Swedish or else Finnish) before that.
 */
const texts = {
  fi: {
    loading: 'Ladataan…',
    notPaired: 'Laitetta ei ole pariliitetty',
    notPairedHelp: 'Skannaa iDispatchin mobiilisovellusvastaanottajan QR-koodi laitteen kameralla.',
    noConnection: 'Ei yhteyttä palvelimeen',
    retry: 'Yritä uudelleen',
    noStatus: 'Ei tilaa',
    connectionLost: 'Ei yhteyttä palvelimeen. Tilaa ei voi vaihtaa ennen kuin yhteys palaa.',
    statusRefused: 'Tilaa ei voitu vaihtaa, koska yksikön tila oli jo muuttunut.',
    statusFailed: 'Tilaa ei voitu vaihtaa, koska palvelimeen ei saatu yhteyttä.',
    changeStatus: 'Vaihda tila',
  },
  sv: {
    loading: 'Laddar…',
    notPaired: 'Enheten är inte parkopplad',
    notPairedHelp: 'Skanna QR-koden för mobilappen i iDispatch med enhetens kamera.',
    noConnection: 'Ingen anslutning till servern',
    retry: 'Försök igen',
    noStatus: 'Ingen status',
    connectionLost: 'Ingen anslutning till servern. Statusen kan inte ändras förrän anslutningen är tillbaka.',
    statusRefused: 'Statusen kunde inte ändras, eftersom enhetens status redan hade ändrats.',
    statusFailed: 'Statusen kunde inte ändras, eftersom servern inte gick att nå.',
    changeStatus: 'Ändra status',
  },
} as const;

export type Texts = (typeof texts)[keyof typeof texts];

export function textsFor(locale: string): Texts {
  return locale.toLowerCase().startsWith('sv') ? texts.sv : texts.fi;
}
