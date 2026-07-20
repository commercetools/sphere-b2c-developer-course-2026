'use client';

import { usePreferences } from '@/context/preferences-context';

/** Currency dropdown — options come from the project; changing it sets the active currency. */
export function CurrencySwitch() {
  const { currencies, currency, setCurrency } = usePreferences();
  if (currencies.length <= 1) {
    return <span className="text-sm text-[var(--color-charcoal-light)]">{currency}</span>;
  }
  return (
    <select
      aria-label="Currency"
      value={currency}
      onChange={(e) => setCurrency(e.target.value)}
      className="rounded-md border border-[var(--color-border)] bg-white px-2 py-1 text-sm"
    >
      {currencies.map((c) => (
        <option key={c} value={c}>
          {c}
        </option>
      ))}
    </select>
  );
}
