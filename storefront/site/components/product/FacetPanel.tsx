'use client';

import { useCapability } from '@/context/capabilities-context';
import { Lock, ChevronDown } from '@/components/ui/icons';
import { useUnlockDialog } from '@/context/unlock-dialog';

/** Gated on search.facets (Tier-2). Locked → a dimmed control that opens the unlock dialog. */
export function FacetPanel() {
  const { loading, unlocked, meta } = useCapability('search.facets');
  const { open } = useUnlockDialog();
  if (loading) return null;
  if (!unlocked) {
    return (
      <button
        onClick={() => open(meta)}
        className="rounded-md border border-dashed border-[var(--color-border)] px-3 py-1.5 text-sm text-[var(--color-charcoal-light)]"
      >
        <Lock size={13} className="mr-1 inline align-[-2px]" />Filters
      </button>
    );
  }
  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="text-[var(--color-charcoal-light)]">Filter:</span>
      {['Colour', 'Material', 'Price'].map((f) => (
        <button key={f} className="rounded-md border border-[var(--color-border)] bg-white px-2.5 py-1">
          {f}<ChevronDown size={13} className="ml-1 inline align-[-2px]" />
        </button>
      ))}
    </div>
  );
}
