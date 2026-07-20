'use client';

import { useState } from 'react';
import { Lock, ChevronDown } from '@/components/ui/icons';
import useSWR from 'swr';
import { bffGet } from '@/lib/bff/client';
import { useCapability } from '@/context/capabilities-context';
import { useUnlockDialog } from '@/context/unlock-dialog';
import { usePreferences } from '@/context/preferences-context';

interface StoreView {
  key: string;
  name: string;
  languages: string[];
  countries: string[];
  channelKeys: string[];
}

/**
 * Header "Stores" list, gated on distribution.stores (Task 1.2). Locked → dimmed pill that opens the
 * unlock dialog. Unlocked → lists the real stores from GET /api/stores. This is the visible storefront
 * payoff for the "List Stores" task.
 */
export function StoreSelector() {
  const { loading, unlocked, meta } = useCapability('distribution.stores');
  const { open } = useUnlockDialog();
  const { selectStore, storeName } = usePreferences();
  const [openList, setOpenList] = useState(false);
  // Only fetch once the task is implemented.
  const { data } = useSWR(unlocked ? 'stores' : null, (p) => bffGet<StoreView[]>(p));

  if (loading) return null;

  if (!unlocked) {
    return (
      <button
        onClick={() => open(meta)}
        className="rounded-md border border-dashed border-[var(--color-border)] px-2 py-1 text-sm text-[var(--color-charcoal-light)]"
        title={`Unlocks in ${meta.session} — ${meta.sessionName}`}
      >
        <Lock size={13} className="mr-1 inline align-[-2px]" />Stores
      </button>
    );
  }

  const stores = data?.ok ? (data.data ?? []) : [];

  return (
    <div className="relative">
      <button
        onClick={() => setOpenList((o) => !o)}
        className="rounded-md border border-[var(--color-border)] bg-white px-2 py-1 text-sm hover:border-[var(--color-terra)]"
      >
        {storeName ?? 'Stores'}<ChevronDown size={13} className="ml-1 inline align-[-2px]" />
      </button>
      {openList ? (
        <div className="absolute right-0 z-30 mt-1 w-64 rounded-lg border border-[var(--color-border)] bg-white p-2 shadow-lg">
          {stores.length === 0 ? (
            <p className="px-2 py-1 text-sm text-[var(--color-charcoal-light)]">No stores configured.</p>
          ) : (
            stores.map((s) => (
              <button
                key={s.key}
                onClick={() => {
                  selectStore(s);
                  setOpenList(false);
                }}
                className={`block w-full rounded-md px-2 py-1.5 text-left hover:bg-[var(--color-cream-dark)] ${
                  storeName === s.name ? 'bg-[var(--color-cream-dark)]' : ''
                }`}
              >
                <div className="text-sm font-medium">{s.name}</div>
                <div className="text-xs text-[var(--color-charcoal-light)]">
                  {s.countries.join(', ') || '—'} · {s.languages.join(', ') || '—'}
                </div>
              </button>
            ))
          )}
        </div>
      ) : null}
    </div>
  );
}
