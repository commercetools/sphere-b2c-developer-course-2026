'use client';

import { usePreferences } from '@/context/preferences-context';
import { countryLabel } from '@/lib/utils';

/**
 * Country dropdown — options come from the project (or active store). Country is an independent
 * price-selection axis: changing it re-fetches PLP/PDP prices for that country (falling back to the
 * currency price when no country-specific price exists).
 */
export function CountrySwitch() {
  const { countries, country, setCountry } = usePreferences();
  if (countries.length <= 1) {
    return <span className="text-sm text-[var(--color-charcoal-light)]">{countryLabel(country)}</span>;
  }
  return (
    <select
      aria-label="Country"
      value={country}
      onChange={(e) => setCountry(e.target.value)}
      className="rounded-md border border-[var(--color-border)] bg-white px-2 py-1 text-sm"
    >
      {countries.map((c) => (
        <option key={c} value={c}>
          {countryLabel(c)}
        </option>
      ))}
    </select>
  );
}
