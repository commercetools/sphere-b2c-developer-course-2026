'use client';

import { useState } from 'react';
import { useLocale } from 'next-intl';
import useSWR from 'swr';
import { bffGet } from '@/lib/bff/client';
import { usePreferences } from '@/context/preferences-context';
import { formatMoney } from '@/lib/utils';

/**
 * Task 2.6 (catalog.variantMatrix): the PDP variant selection matrix. Fetches
 * GET /api/products/{key}/variants, renders one chip row per selectable axis, and resolves the
 * chosen combination to a concrete variant (SKU + price + availability). "Which combinations exist"
 * is implicit in the returned rows — an impossible combination simply has no row.
 */
interface Axis {
  name: string;
  options: string[];
}
interface VariantOption {
  sku: string;
  selections: Record<string, string>;
  price?: { currencyCode: string; centAmount: number } | null;
  imageUrl?: string | null;
  available?: boolean | null;
}
interface VariantMatrixView {
  key: string;
  defaultSku: string | null;
  axes: Axis[];
  variants: VariantOption[];
}

// Shown while the capability is locked, so the dimmed gate still looks real.
const SAMPLE: VariantMatrixView = {
  key: 'sample',
  defaultSku: 's-1',
  axes: [{ name: 'Colour', options: ['Slate Gray', 'Royal Blue', 'Peru'] }],
  variants: [
    { sku: 's-1', selections: { Colour: 'Slate Gray' } },
    { sku: 's-2', selections: { Colour: 'Royal Blue' } },
    { sku: 's-3', selections: { Colour: 'Peru' } },
  ],
};

export function VariantSelector({ productKey }: { productKey?: string }) {
  const locale = useLocale();
  const { currency, country, channel } = usePreferences();
  const params = new URLSearchParams({ locale });
  if (currency) params.set('priceCurrency', currency);
  if (country) params.set('priceCountry', country);
  if (channel) params.set('priceChannel', channel);

  const { data } = useSWR(
    productKey ? `products/${productKey}/variants?${params.toString()}` : null,
    (p) => bffGet<VariantMatrixView>(p),
  );
  const matrix = data && data.ok && data.data ? data.data : SAMPLE;

  // Initial selection = the default variant's axis values (the master variant).
  const defaultVariant =
    matrix.variants.find((v) => v.sku === matrix.defaultSku) ?? matrix.variants[0];
  const [selected, setSelected] = useState<Record<string, string>>(
    defaultVariant?.selections ?? {},
  );

  // Resolve the variant whose selections match every axis — else the combination doesn't exist.
  const resolved = matrix.variants.find((v) =>
    matrix.axes.every((a) => v.selections[a.name] === selected[a.name]),
  );

  return (
    <div className="space-y-3">
      {matrix.axes.map((axis) => (
        <div key={axis.name}>
          <div className="mb-1 text-xs uppercase tracking-wide text-[var(--color-charcoal-light)]">
            {axis.name}
            {selected[axis.name] ? <span className="normal-case">: {selected[axis.name]}</span> : null}
          </div>
          <div className="flex flex-wrap gap-2">
            {axis.options.map((opt) => {
              const active = selected[axis.name] === opt;
              return (
                <button
                  key={opt}
                  onClick={() => setSelected((s) => ({ ...s, [axis.name]: opt }))}
                  className={`rounded-lg border px-3 py-1.5 text-sm transition ${
                    active
                      ? 'border-[var(--color-terra)] bg-[var(--color-terra)]/10 text-[var(--color-terra)]'
                      : 'border-[var(--color-border)] bg-white hover:border-[var(--color-terra)]'
                  }`}
                >
                  {opt}
                </button>
              );
            })}
          </div>
        </div>
      ))}

      <div className="text-xs text-[var(--color-charcoal-light)]">
        {resolved ? (
          <>
            Selected variant <span className="font-mono">{resolved.sku}</span>
            {resolved.price
              ? ` · ${formatMoney(resolved.price.centAmount, resolved.price.currencyCode, locale)}`
              : ''}
            {resolved.available === false ? ' · out of stock' : ''}
          </>
        ) : (
          'That combination isn’t available'
        )}
      </div>
    </div>
  );
}
