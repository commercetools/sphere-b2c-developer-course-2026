'use client';

import { useEffect, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import { Search, Lock } from '@/components/ui/icons';
import { useRouter } from '@/i18n/routing';
import { useCapability } from '@/context/capabilities-context';
import { useUnlockDialog } from '@/context/unlock-dialog';

/**
 * Storefront search box. Sets the `?q=` param on the products PLP, where the composed search reads it.
 * Gated on search.fullText (Task 3.3): until it unlocks the box is a locked affordance that opens the
 * unlock dialog; once unlocked it submits the query. Prefilled from the current `?q=` so the box
 * reflects the active search.
 */
export function SearchBar() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { loading, unlocked, meta } = useCapability('search.fullText');
  const { open } = useUnlockDialog();
  const [q, setQ] = useState('');

  // Reflect the active query (e.g. on load, or when navigated to /products?q=…).
  useEffect(() => {
    setQ(searchParams.get('q') ?? '');
  }, [searchParams]);

  if (loading) return null;

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!unlocked) {
          open(meta);
          return;
        }
        router.push(`/products${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`);
      }}
      className={`flex flex-1 items-center gap-2 rounded-full border border-[var(--color-border)] bg-white px-4 py-2 ${
        unlocked ? '' : 'opacity-70'
      }`}
    >
      {unlocked ? (
        <Search size={16} className="shrink-0 text-[var(--color-charcoal-light)]" />
      ) : (
        <button type="button" onClick={() => open(meta)} title={`Unlocks in ${meta.session}${meta.sessionName ? ` — ${meta.sessionName}` : ''}`}>
          <Lock size={16} className="shrink-0 text-[var(--color-charcoal-light)]" />
        </button>
      )}
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        onFocus={unlocked ? undefined : () => open(meta)}
        readOnly={!unlocked}
        placeholder={unlocked ? 'Search furniture, décor, lighting…' : 'Search unlocks in Session 3 — Find It'}
        className="w-full bg-transparent text-sm outline-none placeholder:text-[var(--color-charcoal-light)]"
      />
    </form>
  );
}
