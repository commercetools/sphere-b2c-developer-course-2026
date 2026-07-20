'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { useLocale } from 'next-intl';
import { useSearchParams } from 'next/navigation';
import { bffGet } from '@/lib/bff/client';
import { usePreferences } from '@/context/preferences-context';
import { COUNTRY_CONFIG } from '@/lib/utils';
import { ProductCard } from './ProductCard';
import { SortDropdown } from './SortDropdown';
import { FacetPanel } from './FacetPanel';
import { ProductView } from './types';
import { SAMPLE_PRODUCTS } from './samples';

/**
 * The PLP. Gated on catalog.plp at the page level; inside, sort and facets are individually
 * gated (search.sort / search.facets) so those Tier-1/Tier-2 controls light up independently.
 * When a `?category=<key>` is present, lists that category's products (incl. subcategories).
 */
export function ProductGrid() {
  const locale = useLocale();
  const { currency } = usePreferences();
  const country = COUNTRY_CONFIG[locale]?.country;
  const category = useSearchParams().get('category');
  const [sort, setSort] = useState('');
  // locale + price selection (currency/country) in the query & SWR key so names resolve to the
  // selected language and cards show the selected-currency price.
  const params = new URLSearchParams({ locale });
  if (currency) params.set('priceCurrency', currency);
  if (country) params.set('priceCountry', country);
  if (sort) params.set('sort', sort);
  // Category-filtered listing when a category is selected, else the full catalogue.
  const path = category
    ? `categories/${encodeURIComponent(category)}/products?${params.toString()}`
    : `products?${params.toString()}`;
  const { data } = useSWR(path, (p) => bffGet<ProductView[]>(p));
  // Live products when implemented; sample catalogue otherwise so the (dimmed) grid looks real.
  const products = data && data.ok && data.data && data.data.length > 0 ? data.data : SAMPLE_PRODUCTS;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between gap-3">
        <FacetPanel />
        <SortDropdown value={sort} onChange={setSort} />
      </div>
      <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
        {products.map((p) => (
          <ProductCard key={p.key} product={p} />
        ))}
      </div>
    </div>
  );
}
