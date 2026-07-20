'use client';

import { FeatureGate } from '@/components/ui/FeatureGate';
import { CategoryBar } from './CategoryBar';

/** PLP category navigation — the shared category → subcategory bar (see {@link CategoryBar}). */
export function CategoryNav() {
  return (
    <FeatureGate capability="catalog.categories" title="Category navigation">
      <CategoryBar />
    </FeatureGate>
  );
}
