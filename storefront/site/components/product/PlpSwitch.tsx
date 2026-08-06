'use client';

import { useCapability } from '@/context/capabilities-context';
import { FeatureGate } from '@/components/ui/FeatureGate';
import { ProductGrid } from './ProductGrid';
import { SearchPlp } from './SearchPlp';

/**
 * THE PLP cutover — one PLP, capability-switched. While the composed store-scoped search PLP
 * (search.plpV2) is locked, the storefront shows the Session-2 catalogue grid (gated on catalog.plp).
 * The moment search.plpV2 unlocks, it flips to the search-powered SearchPlp (facets, sort, paging,
 * post-filtering) — no page reload, driven live by the capabilities poll.
 */
export function PlpSwitch() {
  const { loading, unlocked } = useCapability('search.plpV2');
  if (loading) return null;
  if (unlocked) return <SearchPlp />;
  return (
    <FeatureGate capability="catalog.plp" title="Product listing">
      <ProductGrid />
    </FeatureGate>
  );
}
