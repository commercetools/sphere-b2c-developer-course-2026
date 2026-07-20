'use client';

import { useState } from 'react';
import { Bed, Sofa, Armchair, Lightbulb, Flame, Utensils, Leaf, Droplet, Layers, Sparkles, Tag, ChevronDown } from '@/components/ui/icons';
import useSWR from 'swr';
import { useLocale } from 'next-intl';
import { bffGet } from '@/lib/bff/client';
import { Link } from '@/i18n/routing';
import { CategoryView } from './types';
import { SAMPLE_CATEGORIES } from './samples';

const ICONS: Array<[string, typeof Tag]> = [
  ['bed', Bed], ['bedroom', Bed], ['sofa', Sofa], ['living', Sofa], ['chair', Armchair],
  ['light', Lightbulb], ['lamp', Lightbulb], ['candle', Flame], ['deco', Flame], ['décor', Flame],
  ['kitchen', Utensils], ['garden', Leaf], ['bath', Droplet], ['rug', Layers], ['new', Sparkles],
];
function iconFor(name: string): typeof Tag {
  const n = (name ?? '').toLowerCase();
  for (const [k, Icon] of ICONS) if (n.includes(k)) return Icon;
  return Tag;
}

/**
 * Category → subcategory navigation bar. Renders the top-level categories as pills; a category with
 * children reveals its subcategories in a dropdown on hover/focus. Fed by the BFF category tree
 * (GET /api/categories returns top-level nodes with `children`); falls back to flat samples.
 */
export function CategoryBar() {
  const locale = useLocale();
  const { data } = useSWR(`categories?locale=${locale}`, (p) => bffGet<CategoryView[]>(p));
  const cats = data && data.ok && data.data && data.data.length > 0 ? data.data : SAMPLE_CATEGORIES;
  return (
    <nav className="flex flex-wrap gap-2">
      {cats.map((c) => (
        <CategoryPill key={c.key} category={c} />
      ))}
    </nav>
  );
}

function CategoryPill({ category }: { category: CategoryView }) {
  const [open, setOpen] = useState(false);
  const children = category.children ?? [];
  const Icon = iconFor(category.name);
  return (
    <div
      className="relative"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <Link
        href={{ pathname: '/products', query: { category: category.key } }}
        className="flex items-center gap-1.5 rounded-full border border-[var(--color-border)] bg-white px-3 py-1.5 text-sm hover:border-[var(--color-terra)]"
      >
        <Icon size={15} className="text-[var(--color-charcoal-light)]" />
        <span>{category.name}</span>
        {children.length > 0 ? <ChevronDown size={14} className="text-[var(--color-charcoal-light)]" /> : null}
      </Link>
      {open && children.length > 0 ? (
        <div className="absolute left-0 top-full z-20 mt-1 min-w-44 rounded-xl border border-[var(--color-border)] bg-white p-1 shadow-lg">
          {children.map((sub) => (
            <Link
              key={sub.key}
              href={{ pathname: '/products', query: { category: sub.key } }}
              className="block rounded-lg px-3 py-1.5 text-sm hover:bg-[var(--color-cream-dark)]"
            >
              {sub.name}
            </Link>
          ))}
        </div>
      ) : null}
    </div>
  );
}
