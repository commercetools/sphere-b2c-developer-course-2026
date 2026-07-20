import { Hero } from '@/components/product/Hero';
import { CategoryCards } from '@/components/product/CategoryCards';
import { ProductGrid } from '@/components/product/ProductGrid';
import { FeatureGate } from '@/components/ui/FeatureGate';
import { CapabilityBoard } from '@/components/ui/CapabilityBoard';

export default function HomePage() {
  return (
    <div className="space-y-12">
      <Hero />

      <CategoryCards />

      <section className="space-y-4">
        <h2 className="font-display text-2xl">Featured</h2>
        <FeatureGate capability="catalog.plp" title="Product listing">
          <ProductGrid />
        </FeatureGate>
      </section>

      {/* Training aid — tucked away so the storefront reads as a real shop. */}
      <details className="rounded-xl border border-[var(--color-border)] bg-white/60">
        <summary className="cursor-pointer px-4 py-3 text-sm text-[var(--color-charcoal-light)]">
          Training · feature status board
        </summary>
        <div className="p-3 pt-0">
          <CapabilityBoard />
        </div>
      </details>
    </div>
  );
}
