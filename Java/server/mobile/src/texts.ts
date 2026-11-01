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
  },
  sv: {
    loading: 'Laddar…',
    notPaired: 'Enheten är inte parkopplad',
    notPairedHelp: 'Skanna QR-koden för mobilappen i iDispatch med enhetens kamera.',
    noConnection: 'Ingen anslutning till servern',
    retry: 'Försök igen',
    noStatus: 'Ingen status',
  },
} as const;

export type Texts = (typeof texts)[keyof typeof texts];

export function textsFor(locale: string): Texts {
  return locale.toLowerCase().startsWith('sv') ? texts.sv : texts.fi;
}
