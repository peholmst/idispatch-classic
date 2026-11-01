import { describe, expect, it } from 'vitest';
import { textsFor } from './texts';

describe('the language of the app', () => {
  it('is Swedish for Swedish, and Finnish otherwise', () => {
    expect(textsFor('sv').retry).toBe('Försök igen');
    expect(textsFor('sv-FI').retry).toBe('Försök igen');
    expect(textsFor('fi').retry).toBe('Yritä uudelleen');
    expect(textsFor('en-US').retry).toBe('Yritä uudelleen');
  });
});
