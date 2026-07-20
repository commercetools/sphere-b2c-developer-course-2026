'use client';

import { useState } from 'react';
import { Search } from '@/components/ui/icons';
import { useRouter } from '@/i18n/routing';

/** Storefront search box — routes to the PLP. (Full search is a later gated task.) */
export function SearchBar() {
  const router = useRouter();
  const [q, setQ] = useState('');
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        router.push(`/products${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`);
      }}
      className="flex flex-1 items-center gap-2 rounded-full border border-[var(--color-border)] bg-white px-4 py-2"
    >
      <Search size={16} className="shrink-0 text-[var(--color-charcoal-light)]" />
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Search furniture, décor, lighting…"
        className="w-full bg-transparent text-sm outline-none placeholder:text-[var(--color-charcoal-light)]"
      />
    </form>
  );
}
