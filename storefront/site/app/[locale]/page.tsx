import { Hero } from '@/components/product/Hero';
import { CategoryCards } from '@/components/product/CategoryCards';
import { PlpSwitch } from '@/components/product/PlpSwitch';
import { CapabilityBoard } from '@/components/ui/CapabilityBoard';

export default function HomePage() {
  return (
    <div className="space-y-12">
      <Hero />

      <CategoryCards />

      <section className="space-y-4">
        <h2 className="font-display text-2xl">Featured</h2>
        {/* Catalogue grid (catalog.plp) until search.plpV2 unlocks, then the store-scoped discovery
            PLP — flips live via the capabilities poll, no storefront change or reload needed. */}
        <PlpSwitch />
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
