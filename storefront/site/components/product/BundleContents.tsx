'use client';

import Image from 'next/image';
import { useLocale } from 'next-intl';
import useSWR from 'swr';
import { Package } from '@/components/ui/icons';
import { bffGet } from '@/lib/bff/client';
import { usePreferences } from '@/context/preferences-context';
import { COUNTRY_CONFIG, formatMoney } from '@/lib/utils';

/**
 * Task 2.7 (catalog.bundles): the PDP bundle section. Fetches GET /api/products/{key}/bundle, which
 * expands the bundle's component references and rolls up the total price. A non-bundle product
 * resolves to isBundle=false — this section then renders nothing (only bundles show it).
 */
interface BundleComponent {
  key: string;
  name: string;
  slug: string;
  price?: { currencyCode: string; centAmount: number } | null;
  imageUrl?: string | null;
}
interface BundleView {
  key: string;
  name: string;
  isBundle: boolean;
  totalPrice?: { currencyCode: string; centAmount: number } | null;
  components: BundleComponent[];
}

// Shown while the capability is locked, so the dimmed gate still looks real.
const SAMPLE: BundleView = {
  key: 'sample',
  name: 'Sample Bundle',
  isBundle: true,
  totalPrice: { currencyCode: 'EUR', centAmount: 3698 },
  components: [
    { key: 'c1', name: 'Luxe Pillow Cover', slug: '', price: { currencyCode: 'EUR', centAmount: 2599 } },
    { key: 'c2', name: 'Lana Pillow Cover', slug: '', price: { currencyCode: 'EUR', centAmount: 1099 } },
  ],
};

export function BundleContents({ productKey }: { productKey?: string }) {
  const locale = useLocale();
  const { currency } = usePreferences();
  const country = COUNTRY_CONFIG[locale]?.country;
  const params = new URLSearchParams({ locale });
  if (currency) params.set('priceCurrency', currency);
  if (country) params.set('priceCountry', country);

  const { data } = useSWR(
    productKey ? `products/${productKey}/bundle?${params.toString()}` : null,
    (p) => bffGet<BundleView>(p),
  );
  const live = data && data.ok && data.data ? data.data : null;

  // Real data says this product isn't a bundle → render nothing (only bundles show this section).
  if (live && !live.isBundle) return null;
  const bundle = live ?? SAMPLE;

  return (
    <div className="rounded-xl border border-[var(--color-border)] bg-white p-4">
      <div className="mb-3 text-xs uppercase tracking-wide text-[var(--color-charcoal-light)]">
        This set includes
      </div>
      <ul className="space-y-3">
        {bundle.components.map((c) => (
          <li key={c.key} className="flex items-center gap-3">
            <div className="relative h-12 w-12 overflow-hidden rounded-lg bg-[var(--color-cream-dark)]">
              {c.imageUrl ? (
                <Image src={c.imageUrl} alt={c.name} fill sizes="48px" className="object-cover" />
              ) : (
                <div className="grid h-full place-items-center text-[var(--color-charcoal-light)]/40">
                  <Package size={18} />
                </div>
              )}
            </div>
            <span className="flex-1 text-sm">{c.name}</span>
            <span className="text-sm text-[var(--color-charcoal-light)]">
              {c.price ? formatMoney(c.price.centAmount, c.price.currencyCode, locale) : '—'}
            </span>
          </li>
        ))}
      </ul>
      <div className="mt-3 flex items-center justify-between border-t border-[var(--color-border)] pt-3 text-sm font-semibold">
        <span>Bundle total</span>
        <span>
          {bundle.totalPrice
            ? formatMoney(bundle.totalPrice.centAmount, bundle.totalPrice.currencyCode, locale)
            : '—'}
        </span>
      </div>
    </div>
  );
}
