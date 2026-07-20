'use client';

import Image from 'next/image';
import { Package, Lock, Unlock, Star } from '@/components/ui/icons';
import { useLocale } from 'next-intl';
import { Link } from '@/i18n/routing';
import { formatMoney } from '@/lib/utils';
import { useCapability } from '@/context/capabilities-context';
import { useUnlockDialog } from '@/context/unlock-dialog';
import { ProductView } from './types';
import { AddToCartButton } from './AddToCartButton';

export function ProductCard({ product }: { product: ProductView }) {
  const locale = useLocale();
  // The card links to the PDP — gate that on catalog.pdp (Task 2.2). Until the PDP read is
  // implemented, the card shows an Unlock affordance instead of navigating to a dead page.
  const { unlocked: pdpUnlocked, meta } = useCapability('catalog.pdp');
  const { open } = useUnlockDialog();
  const href = `/products/${product.slug || product.key}`;
  const unlockTitle = `Unlocks in ${meta.session}${meta.sessionName ? ` — ${meta.sessionName}` : ''}`;

  const media = (
    <div className="relative aspect-square bg-[var(--color-cream-dark)]">
      {product.imageUrl ? (
        <Image
          src={product.imageUrl}
          alt={product.name}
          fill
          sizes="(max-width: 768px) 50vw, 25vw"
          className="object-cover"
        />
      ) : (
        <div className="grid h-full place-items-center text-[var(--color-charcoal-light)]/30">
          <Package size={36} />
        </div>
      )}
      {!pdpUnlocked ? (
        <span className="absolute right-2 top-2 rounded-full bg-white/90 px-1.5 py-0.5 text-xs shadow ring-1 ring-[var(--color-border)]">
          <Lock size={12} className="text-[var(--color-charcoal)]" />
        </span>
      ) : null}
    </div>
  );

  return (
    <div className="product-card flex flex-col overflow-hidden rounded-2xl border border-[var(--color-border)] bg-white">
      {pdpUnlocked ? (
        <Link href={href} className="block">
          {media}
        </Link>
      ) : (
        <button type="button" onClick={() => open(meta)} className="block w-full" title={unlockTitle}>
          {media}
        </button>
      )}
      <div className="flex flex-1 flex-col gap-1.5 p-4">
        {pdpUnlocked ? (
          <Link href={href} className="font-medium leading-snug hover:text-[var(--color-terra)]">
            {product.name}
          </Link>
        ) : (
          <button
            type="button"
            onClick={() => open(meta)}
            className="text-left font-medium leading-snug hover:text-[var(--color-terra)]"
          >
            {product.name}
          </button>
        )}
        <div className="text-xs text-[#d9a441]">
          <span className="inline-flex items-center gap-0.5 align-[-2px]">{[0, 1, 2, 3].map((i) => (<Star key={i} size={12} filled />))}<Star size={12} /></span> <span className="text-[var(--color-charcoal-light)]">(24)</span>
        </div>
        <div className="mt-auto flex items-center justify-between gap-2 pt-2">
          <span className="font-semibold">
            {product.price
              ? formatMoney(product.price.centAmount, product.price.currencyCode, locale)
              : '—'}
          </span>
          {pdpUnlocked ? (
            <AddToCartButton />
          ) : (
            <button
              type="button"
              onClick={() => open(meta)}
              className="rounded-full bg-white px-3 py-1 text-xs font-medium ring-1 ring-[var(--color-border)] hover:ring-[var(--color-terra)]"
              title={unlockTitle}
            >
              <Unlock size={12} className="mr-1 inline align-[-1px]" />Unlock
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
