'use client';

import { FeatureGate } from '@/components/ui/FeatureGate';
import { CategoryBar } from './CategoryBar';

/** Homepage "Shop by category" — a compact category → subcategory bar (see {@link CategoryBar}). */
export function CategoryCards() {
  return (
    <section className="space-y-3">
      <h2 className="font-display text-xl">Shop by category</h2>
      <FeatureGate capability="catalog.categories" title="Category navigation">
        <CategoryBar />
      </FeatureGate>
    </section>
  );
}
