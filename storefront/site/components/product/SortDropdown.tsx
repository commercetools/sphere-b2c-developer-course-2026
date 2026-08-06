'use client';

import { useCapability } from '@/context/capabilities-context';
import { Lock } from '@/components/ui/icons';
import { useUnlockDialog } from '@/context/unlock-dialog';

/**
 * Sort control. When a `capability` is supplied it is gated on it (locked → a dimmed control that
 * opens the unlock dialog); when omitted it renders the active select unconditionally — the S3
 * SearchPlp uses it that way, since sort is part of the already-unlocked composed PLP (search.plpV2).
 */
export function SortDropdown({
  value,
  onChange,
  capability,
}: {
  value: string;
  onChange: (v: string) => void;
  capability?: string;
}) {
  const { loading, unlocked, meta } = useCapability(capability ?? '');
  const { open } = useUnlockDialog();
  const gated = Boolean(capability);
  if (gated && loading) return null;
  if (gated && !unlocked) {
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
