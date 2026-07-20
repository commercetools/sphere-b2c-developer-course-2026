'use client';

import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { useLocale } from 'next-intl';
import useSWR from 'swr';
import { routing, usePathname, useRouter } from '@/i18n/routing';
import { bffGet } from '@/lib/bff/client';
import { COUNTRY_CONFIG, CURRENCY_BY_COUNTRY } from '@/lib/utils';

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
}

interface PreferencesValue {
  /** Active store name (once one is selected), else the project name / null. */
  storeName: string | null;
  /** Available language options (locales) — the active store's, else the project's. */
  languages: string[];
  /** Available currency options — the project's. */
  currencies: string[];
  /** Active language (the next-intl locale) and currency. */
  language: string;
  currency: string;
  setLanguage: (locale: string) => void;
  setCurrency: (currency: string) => void;
  /** Switch to a store: sets its default language + currency and narrows the language options. */
  selectStore: (store: StoreLite) => void;
}

const PreferencesContext = createContext<PreferencesValue | null>(null);

const FALLBACK_LANGS = Object.keys(COUNTRY_CONFIG);
const FALLBACK_CURRENCIES = [...new Set(Object.values(COUNTRY_CONFIG).map((c) => c.currency))];

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

  const [storeName, setStoreName] = useState<string | null>(null);
  const [storeLangs, setStoreLangs] = useState<string[] | null>(null);
  const [currency, setCurrencyState] = useState<string>('');

  // Default the currency once the project's currencies are known (prefer the one for the active locale).
  useEffect(() => {
    if (!currency && currencies.length > 0) {
      const localeCurrency = COUNTRY_CONFIG[locale]?.currency;
      setCurrencyState(localeCurrency && currencies.includes(localeCurrency) ? localeCurrency : currencies[0]);
    }
  }, [currency, currencies, locale]);

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
    languages: languages.length ? languages : FALLBACK_LANGS,
    currencies,
    language: locale,
    currency: currency || currencies[0] || 'EUR',
    setLanguage,
    setCurrency: setCurrencyState,
    selectStore: (store) => {
      setStoreName(store.name);
      const langs = (store.languages ?? []).filter(isSupportedLocale);
      setStoreLangs(langs.length ? langs : null);
      if (langs.length) setLanguage(langs[0]); // store's default language
      const byCountry = store.countries?.[0] ? CURRENCY_BY_COUNTRY[store.countries[0]] : undefined;
      setCurrencyState(byCountry && currencies.includes(byCountry) ? byCountry : currencies[0]);
    },
  };

  return <PreferencesContext.Provider value={value}>{children}</PreferencesContext.Provider>;
}

export function usePreferences(): PreferencesValue {
  const ctx = useContext(PreferencesContext);
  if (!ctx) throw new Error('usePreferences must be used within PreferencesProvider');
  return ctx;
}
