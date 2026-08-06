'use client';

import { useEffect, useState } from 'react';
import { useLocale } from 'next-intl';
import { useCapability } from '@/context/capabilities-context';
import { Lock, ChevronDown } from '@/components/ui/icons';
import { useUnlockDialog } from '@/context/unlock-dialog';
import { formatMoney } from '@/lib/utils';
import { FacetView } from './types';

interface FacetPanelProps {
  /** When provided (rail mode), render the real facets returned by the composed PLP. Omit for the
   *  legacy Session-2 locked control. */
  facets?: FacetView[];
  selectedColours?: string[];
  /** Selected price window, in minor units (centAmount), or null when unset. */
  priceFrom?: number | null;
  priceTo?: number | null;
  currencyCode?: string;
  onToggleColour?: (key: string) => void;
  onPriceChange?: (from: number | null, to: number | null) => void;
}

/**
 * The facet rail. Two modes:
 *  - LEGACY (no `facets` prop): the Session-2 static control, gated on search.facets — locked shows a
 *    dimmed "Filters" button that opens the unlock dialog.
 *  - RAIL (given `facets`): renders the real colour buckets + a price range slider from the PLP's
 *    facet stats. Displaying buckets needs only the composed PLP; APPLYING a filter is gated on
 *    search.postFilter (so counts stay stable) — locked, the controls open the unlock dialog.
 */
export function FacetPanel(props: FacetPanelProps) {
  if (props.facets === undefined) return <LegacyFacetPanel />;
  return <FacetRail {...props} />;
}

function LegacyFacetPanel() {
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

function labelFor(key: string): string {
  return key ? key.charAt(0).toUpperCase() + key.slice(1) : key;
}

function FacetRail({
  facets = [],
  selectedColours = [],
  priceFrom = null,
  priceTo = null,
  currencyCode = 'EUR',
  onToggleColour,
  onPriceChange,
}: FacetPanelProps) {
  const { unlocked, meta } = useCapability('search.postFilter');
  const { open } = useUnlockDialog();

  const colour = facets.find((f) => f.name === 'colour' || f.name === 'color');
  const stats = facets.find((f) => f.type === 'stats')?.stats ?? null;

  const guard = (fn: () => void) => (unlocked ? fn() : open(meta));

  return (
    <aside className="w-full space-y-6 md:w-52 md:shrink-0">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold">Filters</h2>
        {!unlocked ? (
          <button
            onClick={() => open(meta)}
            className="inline-flex items-center gap-1 text-xs text-[var(--color-charcoal-light)]"
            title={`Unlocks in ${meta.session}${meta.sessionName ? ` — ${meta.sessionName}` : ''}`}
          >
            <Lock size={12} />Unlock
          </button>
        ) : null}
      </div>

      {colour && colour.buckets && colour.buckets.length > 0 ? (
        <div className={unlocked ? '' : 'opacity-60'}>
          <h3 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--color-charcoal-light)]">
            Colour
          </h3>
          <ul className="space-y-1">
            {colour.buckets.map((b) => {
              const checked = selectedColours.includes(b.key);
              return (
                <li key={b.key}>
                  <button
                    onClick={() => guard(() => onToggleColour?.(b.key))}
                    className={`flex w-full items-center justify-between rounded-md px-2 py-1 text-sm hover:bg-[var(--color-cream-dark)] ${
                      checked ? 'font-medium text-[var(--color-terra)]' : ''
                    }`}
                  >
                    <span className="flex items-center gap-2">
                      <span
                        className={`grid h-3.5 w-3.5 place-items-center rounded border ${
                          checked
                            ? 'border-[var(--color-terra)] bg-[var(--color-terra)] text-white'
                            : 'border-[var(--color-border)]'
                        }`}
                      >
                        {checked ? '✓' : ''}
                      </span>
                      {labelFor(b.key)}
                    </span>
                    <span className="text-xs text-[var(--color-charcoal-light)]">{b.count}</span>
                  </button>
                </li>
              );
            })}
          </ul>
        </div>
      ) : null}

      {stats && stats.max > stats.min ? (
        <PriceSlider
          min={stats.min}
          max={stats.max}
          from={priceFrom}
          to={priceTo}
          currencyCode={currencyCode}
          disabled={!unlocked}
          onApply={(f, t) => guard(() => onPriceChange?.(f, t))}
        />
      ) : null}
    </aside>
  );
}

function PriceSlider({
  min,
  max,
  from,
  to,
  currencyCode,
  disabled,
  onApply,
}: {
  min: number;
  max: number;
  from: number | null;
  to: number | null;
  currencyCode: string;
  disabled: boolean;
  onApply: (from: number | null, to: number | null) => void;
}) {
  const locale = useLocale();
  const [lo, setLo] = useState<number>(from ?? min);
  const [hi, setHi] = useState<number>(to ?? max);

  // Keep local thumbs in sync when the applied window (URL) changes or the bounds shift.
  useEffect(() => {
    setLo(from ?? min);
    setHi(to ?? max);
  }, [from, to, min, max]);

  const step = Math.max(1, Math.round((max - min) / 100));
  const dirty = lo !== (from ?? min) || hi !== (to ?? max);

  return (
    <div className={disabled ? 'opacity-60' : ''}>
      <h3 className="mb-2 text-xs font-medium uppercase tracking-wide text-[var(--color-charcoal-light)]">
        Price
      </h3>
      <div className="space-y-2">
        <input
          type="range"
          aria-label="Minimum price"
          min={min}
          max={max}
          step={step}
          value={lo}
          onChange={(e) => setLo(Math.min(Number(e.target.value), hi))}
          className="w-full accent-[var(--color-terra)]"
        />
        <input
          type="range"
          aria-label="Maximum price"
          min={min}
          max={max}
          step={step}
          value={hi}
          onChange={(e) => setHi(Math.max(Number(e.target.value), lo))}
          className="w-full accent-[var(--color-terra)]"
        />
        <div className="flex items-center justify-between text-xs text-[var(--color-charcoal-light)]">
          <span>{formatMoney(lo, currencyCode, locale)}</span>
          <span>{formatMoney(hi, currencyCode, locale)}</span>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => onApply(lo <= min ? null : lo, hi >= max ? null : hi)}
            className="rounded-md bg-[var(--color-terra)] px-3 py-1 text-xs font-medium text-white disabled:opacity-40"
            disabled={!dirty}
          >
            Apply
          </button>
          {from != null || to != null ? (
            <button
              onClick={() => onApply(null, null)}
              className="rounded-md px-3 py-1 text-xs text-[var(--color-charcoal-light)] ring-1 ring-[var(--color-border)]"
            >
              Reset
            </button>
          ) : null}
        </div>
      </div>
    </div>
  );
}
