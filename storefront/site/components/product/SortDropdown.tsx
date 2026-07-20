'use client';

import { useCapability } from '@/context/capabilities-context';
import { Lock } from '@/components/ui/icons';
import { useUnlockDialog } from '@/context/unlock-dialog';

/** Gated on search.sort. Locked → a dimmed control that opens the unlock dialog. */
export function SortDropdown({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  const { loading, unlocked, meta } = useCapability('search.sort');
  const { open } = useUnlockDialog();
  if (loading) return null;
  if (!unlocked) {
    return (
      <button
        onClick={() => open(meta)}
        className="rounded-md border border-dashed border-[var(--color-border)] px-3 py-1.5 text-sm text-[var(--color-charcoal-light)]"
      >
        <Lock size={13} className="mr-1 inline align-[-2px]" />Sort
      </button>
    );
  }
  return (
    <select
      aria-label="Sort products"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      className="rounded-md border border-[var(--color-border)] bg-white px-2 py-1.5 text-sm"
    >
      <option value="">Featured</option>
      <option value="price asc">Price: low to high</option>
      <option value="price desc">Price: high to low</option>
      <option value="name.en asc">Name: A–Z</option>
    </select>
  );
}
