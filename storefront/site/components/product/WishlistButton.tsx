'use client';

import { useState } from 'react';
import { useCapability } from '@/context/capabilities-context';
import { useUnlockDialog } from '@/context/unlock-dialog';
import { useShoppingList } from '@/hooks/useShoppingList';
import { bffGet } from '@/lib/bff/client';
import { Heart } from '@/components/ui/icons';

/**
 * ♡ Save-for-later on the PDP, gated on cart.shoppingList. Resolves the SKU from the variant matrix
 * (the storefront works in product keys; the list saves by SKU) and adds it to the guest wishlist.
 * Locked → opens the unlock dialog.
 */
export function WishlistButton({ productKey, sku }: { productKey?: string; sku?: string }) {
  const { loading, unlocked, meta } = useCapability('cart.shoppingList');
  const { open } = useUnlockDialog();
  const { save } = useShoppingList();
  const [state, setState] = useState<'idle' | 'saving' | 'saved'>('idle');
  if (loading) return null;

  async function resolveSku(): Promise<string | null> {
    if (sku) return sku;
    if (!productKey) return null;
    const r = await bffGet<{ defaultSku?: string; variants?: { sku: string }[] }>(
      `products/${encodeURIComponent(productKey)}/variants`,
    );
    return r.data?.defaultSku ?? r.data?.variants?.[0]?.sku ?? null;
  }

  async function onClick() {
    if (!unlocked) {
      open(meta);
      return;
    }
    if (state === 'saving') return;
    setState('saving');
    try {
      const resolved = await resolveSku();
      if (resolved) {
        await save(resolved, 1);
        setState('saved');
        setTimeout(() => setState('idle'), 1500);
      } else {
        setState('idle');
      }
    } catch {
      setState('idle');
    }
  }

  const label = state === 'saved' ? 'Saved ✓' : 'Save for later';
  return (
    <button
      onClick={onClick}
      disabled={state === 'saving'}
      title={label}
      aria-label={label}
      className={`grid h-11 w-11 shrink-0 place-items-center rounded-lg border border-[var(--color-border)] transition-colors disabled:opacity-60 ${
        state === 'saved'
          ? 'border-[var(--color-terra)] text-[var(--color-terra)]'
          : 'text-[var(--color-charcoal-light)] hover:border-[var(--color-terra)] hover:text-[var(--color-terra)]'
      }`}
    >
      <Heart size={18} />
    </button>
  );
}
