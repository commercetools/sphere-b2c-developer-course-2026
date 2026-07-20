import { Link } from '@/i18n/routing';

export function Hero() {
  return (
    <section className="overflow-hidden rounded-3xl border border-[var(--color-border)] bg-gradient-to-br from-[var(--color-cream-dark)] to-[var(--color-sage)]/30 px-8 py-8 md:px-12 md:py-10">
      <p className="mb-2 text-xs uppercase tracking-[0.2em] text-[var(--color-terra)]">New season · 2026</p>
      <h1 className="font-display max-w-xl text-3xl leading-tight md:text-4xl">
        Considered pieces for calmer living
      </h1>
      <p className="mt-2 max-w-md text-sm text-[var(--color-charcoal-light)]">
        Furniture and home décor, thoughtfully made and built to last — curated by Lifestyle &amp;
        Home Corp.
      </p>
      <Link
        href="/products"
        className="mt-4 inline-block rounded-full bg-[var(--color-terra)] px-5 py-2.5 text-sm font-medium text-white hover:bg-[var(--color-terra-dark)]"
      >
        Shop the collection
      </Link>
    </section>
  );
}
