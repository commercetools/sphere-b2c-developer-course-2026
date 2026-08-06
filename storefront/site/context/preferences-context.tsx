'use client';

import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useLocale } from 'next-intl';
import useSWR from 'swr';
import { routing, usePathname, useRouter } from '@/i18n/routing';
import { bffGet } from '@/lib/bff/client';
import { COUNTRY_CONFIG, CURRENCY_BY_COUNTRY, COUNTRY_BY_CURRENCY } from '@/lib/utils';

interface ProjectView {
  key: string;
  name: string;
  currencies: string[];
  languages: string[];
  countries: string[];
}

/** Minimal store shape the switcher needs. */
export interface StoreLite {
  key: string;
  name: string;
  languages: string[];
  countries: string[];
  /** The store's distribution-channel keys — the first is the default price channel. */
  channelKeys?: string[];
}

interface PreferencesValue {
  /** Active store name (once one is selected), else the project name / null. */
  storeName: string | null;
  /** Active store KEY (once one is selected), else null. S3 discovery calls pass this as `?store=`. */
  storeKey: string | null;
  /** Available language options (locales) — the active store's, else the project's. */
  languages: string[];
  /** Available currency options — the project's. */
  currencies: string[];
  /** Available country options — the active store's, else the project's (drives price selection). */
  countries: string[];
  /** Distribution-channel keys the active store exposes (empty until a store with channels is chosen). */
  channels: string[];
  /** Active language (the next-intl locale), currency, country, and price channel (key, or null). */
  language: string;
  currency: string;
  /** Country for price selection — independently selectable (a pricing-demo axis), defaulting to the
   *  active currency's country so the initial (currency, country) pair is consistent. */
  country: string;
  /** Active distribution channel key for price selection — the store's selected channel, or null. */
  channel: string | null;
  setLanguage: (locale: string) => void;
  setCurrency: (currency: string) => void;
  setCountry: (country: string) => void;
  setChannel: (channelKey: string | null) => void;
  /** Switch to a store: sets its default language + currency + country and its channel options. */
  selectStore: (store: StoreLite) => void;
}

const PreferencesContext = createContext<PreferencesValue | null>(null);

const FALLBACK_LANGS = Object.keys(COUNTRY_CONFIG);
const FALLBACK_CURRENCIES = [...new Set(Object.values(COUNTRY_CONFIG).map((c) => c.currency))];
const FALLBACK_COUNTRIES = [...new Set(Object.values(COUNTRY_CONFIG).map((c) => c.country))];

function isSupportedLocale(locale: string): boolean {
  return (routing.locales as readonly string[]).includes(locale);
}

/**
 * Holds the shopper's active language + currency. Language is the next-intl locale (drives routing
 * and localized content); currency is a display/selection preference. Selecting a store sets both
 * defaults from the store (language = its first language, currency from its country).
 */
export function PreferencesProvider({ children }: { children: React.ReactNode }) {
  const locale = useLocale();
  const pathname = usePathname();
  const router = useRouter();

  const { data } = useSWR('project', (p) => bffGet<ProjectView>(p));
  const project = data?.ok ? data.data : null;

  const projectLangs = project?.languages?.length ? project.languages : FALLBACK_LANGS;
  const currencies = project?.currencies?.length ? project.currencies : FALLBACK_CURRENCIES;
  const projectCountries = project?.countries?.length ? project.countries : FALLBACK_COUNTRIES;

  const [storeName, setStoreName] = useState<string | null>(null);
  const [storeKey, setStoreKey] = useState<string | null>(null);
  const [storeLangs, setStoreLangs] = useState<string[] | null>(null);
  const [storeCountries, setStoreCountries] = useState<string[] | null>(null);
  const [currency, setCurrencyState] = useState<string>('');
  const [country, setCountryState] = useState<string>('');
  // Distribution channels come from the selected store; the first is the default price channel.
  const [channels, setChannels] = useState<string[]>([]);
  const [channel, setChannelState] = useState<string | null>(null);

  const activeCurrency = currency || currencies[0] || 'EUR';

  // Default the currency once the project's currencies are known (prefer the one for the active locale).
  useEffect(() => {
    if (!currency && currencies.length > 0) {
      const localeCurrency = COUNTRY_CONFIG[locale]?.currency;
      setCurrencyState(localeCurrency && currencies.includes(localeCurrency) ? localeCurrency : currencies[0]);
    }
  }, [currency, currencies, locale]);

  // Default the country independently (pricing-demo axis), seeded from the active currency's country
  // so the initial (currency, country) pair is consistent; the shopper can then change it freely.
  const countries = storeCountries ?? projectCountries;
  useEffect(() => {
    if (!country && countries.length > 0) {
      const byCurrency = COUNTRY_BY_CURRENCY[activeCurrency];
      setCountryState(byCurrency && countries.includes(byCurrency) ? byCurrency : countries[0]);
    }
  }, [country, countries, activeCurrency]);

  const languages = useMemo(
    () => (storeLangs ?? projectLangs).filter(isSupportedLocale),
    [storeLangs, projectLangs],
  );

  const setLanguage = (l: string) => {
    if (isSupportedLocale(l) && l !== locale) {
      router.replace(pathname, { locale: l });
    }
  };

  const value: PreferencesValue = {
    storeName: storeName ?? project?.name ?? null,
    storeKey,
    languages: languages.length ? languages : FALLBACK_LANGS,
    currencies,
    countries,
    channels,
    language: locale,
    currency: activeCurrency,
    country: country || countries[0] || '',
    channel,
    setLanguage,
    setCurrency: setCurrencyState,
    setCountry: setCountryState,
    setChannel: setChannelState,
    selectStore: (store) => {
      setStoreName(store.name);
      setStoreKey(store.key);
      const langs = (store.languages ?? []).filter(isSupportedLocale);
      setStoreLangs(langs.length ? langs : null);
      if (langs.length) setLanguage(langs[0]); // store's default language
      const byCountry = store.countries?.[0] ? CURRENCY_BY_COUNTRY[store.countries[0]] : undefined;
      setCurrencyState(byCountry && currencies.includes(byCountry) ? byCountry : currencies[0]);
      setStoreCountries(store.countries?.length ? store.countries : null);
      if (store.countries?.[0]) setCountryState(store.countries[0]); // store's default country
      // Channel options from the store; default to the first (the store's primary price channel).
      const chans = store.channelKeys ?? [];
      setChannels(chans);
      setChannelState(chans[0] ?? null);
    },
  };

  return <PreferencesContext.Provider value={value}>{children}</PreferencesContext.Provider>;
}

export function usePreferences(): PreferencesValue {
  const ctx = useContext(PreferencesContext);
  if (!ctx) throw new Error('usePreferences must be used within PreferencesProvider');
  return ctx;
}
