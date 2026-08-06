'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { useLocale } from 'next-intl';
import { useSearchParams } from 'next/navigation';
import { bffGet } from '@/lib/bff/client';
import { usePreferences } from '@/context/preferences-context';
import { ProductCard } from './ProductCard';
import { SortDropdown } from './SortDropdown';
import { FacetPanel } from './FacetPanel';
import { ProductPageView, ProductView } from './types';
import { SAMPLE_PRODUCTS } from './samples';

/**
 * The Session-2 PLP (catalog.plp). This is the pre-search fallback grid: it renders until the
 * composed store-scoped search PLP (search.plpV2) unlocks, at which point PlpSwitch cuts over to
 * SearchPlp. Sort/Filters show as locked affordances here that point at the Session-3 work.
 * When a `?category=<key>` is present, lists that category's products (incl. subcategories).
 */
export function ProductGrid() {
  const locale = useLocale();
  const { currency, country, channel } = usePreferences();
  const category = useSearchParams().get('category');
  const [sort, setSort] = useState('');
  // locale + price selection (currency/country/channel) in the query & SWR key so names resolve to
  // the selected language and cards show the price for the shopper's channel + country + currency.
  const params = new URLSearchParams({ locale });
  if (currency) params.set('priceCurrency', currency);
  if (country) params.set('priceCountry', country);
  if (channel) params.set('priceChannel', channel);
  if (sort) params.set('sort', sort);
  // Category-filtered listing when a category is selected, else the full catalogue.
  const path = category
    ? `categories/${encodeURIComponent(category)}/products?${params.toString()}`
    : `products?${params.toString()}`;
  const { data } = useSWR(path, (p) => bffGet<ProductPageView>(p));
  // Live envelope { products, total } when implemented; sample catalogue otherwise so the (dimmed)
  // grid still looks real. Before 2.1 lands (501/empty) we fall back to samples and count those.
  const envelope = data && data.ok && data.data && data.data.products?.length ? data.data : null;
  const products = envelope ? envelope.products : SAMPLE_PRODUCTS;
  const total = envelope ? envelope.total : products.length;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between gap-3">
        <p className="text-sm text-[var(--color-charcoal-light)]">
          {envelope ? `Total ${total} product${total === 1 ? '' : 's'}` : 'Sample catalogue'}
        </p>
        <div className="flex items-center gap-3">
          <FacetPanel />
          <SortDropdown value={sort} onChange={setSort} capability="search.plpV2" />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
        {products.map((p) => (
          <ProductCard key={p.key} product={p} />
        ))}
      </div>
    </div>
  );
}
