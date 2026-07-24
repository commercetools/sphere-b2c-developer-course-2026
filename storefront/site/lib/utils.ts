/**
 * The locales this storefront can route to and localize. Keys drive `routing.locales`,
 * `generateStaticParams`, and the language switcher — so a project language only becomes routable
 * when it has an entry here (plus a `messages/<locale>.json` catalog, with a default-locale fallback
 * in `i18n/request.ts` for anything missing). Keep this in sync with the project's languages.
 */
export const COUNTRY_CONFIG: Record<string, { currency: string; locale: string; country: string; label: string }> = {
  'en-US': { locale: 'en-US', currency: 'USD', country: 'US', label: 'United States' },
  'en-GB': { locale: 'en-GB', currency: 'GBP', country: 'GB', label: 'United Kingdom' },
  'de-DE': { locale: 'de-DE', currency: 'EUR', country: 'DE', label: 'Germany' },
  fr: { locale: 'fr', currency: 'EUR', country: 'FR', label: 'France' },
};

export const DEFAULT_LOCALE = COUNTRY_CONFIG['en-US'];

/** Curated language labels; anything not listed is labelled dynamically via {@link languageLabel}. */
export const LANGUAGE_LABELS: Record<string, string> = {
  'en-US': 'English (US)',
  'en-GB': 'English (UK)',
  'de-DE': 'Deutsch',
  fr: 'Français',
};

/**
 * Human label for a locale code. Curated labels win; otherwise derive one from `Intl.DisplayNames`
 * (so any project language — even one we haven't hand-labelled — reads nicely), falling back to the
 * raw code. Data-driven: adding a project language needs no label edit here.
 */
export function languageLabel(code: string): string {
  if (LANGUAGE_LABELS[code]) return LANGUAGE_LABELS[code];
  try {
    const base = code.split('-')[0];
    const name = new Intl.DisplayNames(['en'], { type: 'language' }).of(base);
    return name ?? code;
  } catch {
    return code;
  }
}

/** Human label for a country/region code, derived from `Intl.DisplayNames` (e.g. "US" → "United States"). */
export function countryLabel(code: string): string {
  try {
    return new Intl.DisplayNames(['en'], { type: 'region' }).of(code) ?? code;
  } catch {
    return code;
  }
}

/** Country → currency (derived from COUNTRY_CONFIG), used to default a store's currency. */
export const CURRENCY_BY_COUNTRY: Record<string, string> = Object.fromEntries(
  Object.values(COUNTRY_CONFIG).map((c) => [c.country, c.currency]),
);

/**
 * Currency → country (derived from COUNTRY_CONFIG). Lets price selection's `priceCountry` follow the
 * selected currency instead of the display locale, so the (currency, country) pair passed to
 * commercetools is always consistent. First country wins if a currency repeats.
 */
export const COUNTRY_BY_CURRENCY: Record<string, string> = Object.values(COUNTRY_CONFIG).reduce(
  (acc, c) => {
    if (!(c.currency in acc)) acc[c.currency] = c.country;
    return acc;
  },
  {} as Record<string, string>,
);

export function formatMoney(centAmount: number, currencyCode: string, locale = 'en-US'): string {
  return new Intl.NumberFormat(locale, { style: 'currency', currency: currencyCode })
    .format(centAmount / 100);
}

export function getLocalizedString(obj: Record<string, string> | undefined, locale: string): string {
  if (!obj) return '';
  return obj[locale] ?? obj[locale.split('-')[0]] ?? Object.values(obj)[0] ?? '';
}
