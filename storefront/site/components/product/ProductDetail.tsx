'use client';

import Image from 'next/image';
import { Package, Star } from '@/components/ui/icons';
import { useLocale } from 'next-intl';
import useSWR from 'swr';
import { bffGet } from '@/lib/bff/client';
import { formatMoney } from '@/lib/utils';
import { usePreferences } from '@/context/preferences-context';
import { Link } from '@/i18n/routing';
import { FeatureGate } from '@/components/ui/FeatureGate';
import { AddToCartButton } from './AddToCartButton';
import { VariantSelector } from './VariantSelector';
import { BundleContents } from './BundleContents';
import { ProductView } from './types';
import { SAMPLE_PRODUCT } from './samples';

/**
 * The PDP. Gated on catalog.pdp; the refinements (scoped pricing, variant matrix, inventory,
 * bundles) each gate on their own capability so they unlock independently.
 */
function ProductDetailInner({ slug }: { slug: string }) {
  const locale = useLocale();
  const { currency, country, channel } = usePreferences();
  const params = new URLSearchParams({ locale });
  if (currency) params.set('priceCurrency', currency);
  if (country) params.set('priceCountry', country);
  if (channel) params.set('priceChannel', channel);
  // Task 2.5 (catalog.localeSlugs): resolve the PDP by its LOCALIZED slug. The URL slug differs per
  // locale (e.g. en `nala-two-seater-sofa` vs de `nala-zweisitzer-sofa`), so we resolve by slug — not
  // key. Until 2.5 is implemented the by-slug read returns 501, so we fall back to the by-key read
  // (Task 2.2), which only resolves when the slug happens to equal the product key (English). That's
  // the visible payoff: before 2.5 a localized (German) URL can't resolve; after 2.5 it does.
  const bySlug = useSWR(`products/by-slug/${slug}?${params.toString()}`, (p) => bffGet<ProductView>(p));
  const slugResolved = bySlug.data && bySlug.data.ok && bySlug.data.data ? bySlug.data.data : null;
  const { data: byKey } = useSWR(
    slugResolved ? null : `products/${slug}?${params.toString()}`,
    (p) => bffGet<ProductView>(p),
  );
  const keyResolved = byKey && byKey.ok && byKey.data ? byKey.data : null;
  // Live product when resolved (by slug, else by key); a sample product otherwise so the PDP looks real.
  const product = slugResolved ?? keyResolved ?? SAMPLE_PRODUCT;

  return (
    <div>
      <nav className="mb-6 text-xs text-[var(--color-charcoal-light)]">
        <Link href="/" className="hover:text-[var(--color-terra)]">Home</Link> ·{' '}
        <Link href="/products" className="hover:text-[var(--color-terra)]">Products</Link> ·{' '}
        <span>{product?.name}</span>
      </nav>

      <div className="grid gap-10 md:grid-cols-2">
        {/* Gallery */}
        <div className="space-y-3">
          <div className="relative aspect-square overflow-hidden rounded-2xl bg-[var(--color-cream-dark)]">
            {product?.imageUrl ? (
              <Image
                src={product.imageUrl}
                alt={product?.name ?? ''}
                fill
                sizes="(max-width: 768px) 100vw, 50vw"
                className="object-cover"
              />
            ) : (
              <div className="grid h-full place-items-center text-[var(--color-charcoal-light)]/30">
                <Package size={48} />
              </div>
            )}
          </div>
          <div className="flex gap-3">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className={`relative grid h-16 w-16 place-items-center overflow-hidden rounded-lg bg-[var(--color-cream-dark)] text-xl ${
                  i === 0 ? 'ring-2 ring-[var(--color-terra)]' : ''
                }`}
              >
                {i === 0 && product?.imageUrl ? (
                  <Image src={product.imageUrl} alt="" fill sizes="64px" className="object-cover" />
                ) : (
                  <Package size={22} className="text-[var(--color-charcoal-light)]" />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Details */}
        <div className="space-y-5">
          <div className="text-xs uppercase tracking-wide text-[var(--color-charcoal-light)]">
            {product?.key}
          </div>
          <h1 className="font-display text-3xl leading-tight">{product?.name}</h1>
          <div className="text-sm text-[#d9a441]">
            <span className="inline-flex items-center gap-0.5 align-[-2px]">{[0, 1, 2, 3].map((i) => (<Star key={i} size={14} filled />))}<Star size={14} /></span> <span className="text-[var(--color-charcoal-light)]">(24 reviews)</span>
          </div>

          <FeatureGate capability="pricing.resolve" title="Price (scoped + discounts)">
            <p className="text-2xl font-semibold">
              {product?.price
                ? formatMoney(product.price.centAmount, product.price.currencyCode, locale)
                : '—'}
            </p>
          </FeatureGate>

          <FeatureGate capability="catalog.variantMatrix" title="Variant selection">
            <VariantSelector productKey={product?.key} />
          </FeatureGate>

          <div className="flex items-center gap-3">
            <span className="text-sm text-[var(--color-charcoal-light)]">Qty</span>
            <div className="flex items-center rounded-lg border border-[var(--color-border)] bg-white">
              <button className="px-3 py-1.5">−</button>
              <span className="px-3">1</span>
              <button className="px-3 py-1.5">+</button>
            </div>
            <FeatureGate capability="inventory.read" title="Availability">
              <InventoryBadge sku={product?.key ?? slug} />
            </FeatureGate>
          </div>

          <div className="pt-1">
            <AddToCartButton full />
          </div>

          <FeatureGate capability="catalog.bundles" title="Bundle contents">
            <BundleContents productKey={product?.key} />
          </FeatureGate>
        </div>
      </div>
    </div>
  );
}

function InventoryBadge({ sku }: { sku: string }) {
  const { data } = useSWR(`inventory/${sku}`, (p) => bffGet<{ isOnStock?: boolean }>(p));
  const onStock = data?.data?.isOnStock;
  return (
    <span className="inline-flex items-center gap-2 text-sm">
      <span className={`h-2 w-2 rounded-full ${onStock ? 'bg-[var(--color-sage)]' : 'bg-[var(--color-charcoal-light)]'}`} />
      {onStock ? 'In stock' : 'Availability'}
    </span>
  );
}

export function ProductDetail({ slug }: { slug: string }) {
  return (
    <FeatureGate capability="catalog.pdp" title="Product detail">
      <ProductDetailInner slug={slug} />
    </FeatureGate>
  );
}
