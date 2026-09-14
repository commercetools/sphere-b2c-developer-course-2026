'use client';

import { useState } from 'react';
import { useCapability } from '@/context/capabilities-context';
import { useUnlockDialog } from '@/context/unlock-dialog';
import { useCart } from '@/hooks/useCart';
import { bffGet } from '@/lib/bff/client';
import { Lock } from '@/components/ui/icons';

/**
 * Add-to-cart, gated on cart.lineItems. When locked it renders dimmed and opens the unlock dialog.
 * When unlocked it resolves the product's SKU (from the variant matrix — the storefront works in
 * product keys, the cart adds by SKU) and calls the real BFF add; SWR then refreshes the cart
 * everywhere by the shared "cart" key.
 */
export function AddToCartButton({
  full = false,
  productKey,
  sku,
  recurring = false,
  recurrencePolicy = 'monthly',
}: {
  full?: boolean;
  productKey?: string;
  sku?: string;
  /** When true, adds a subscription line (recurrenceInfo → recurrencePolicy) instead of a one-off. */
  recurring?: boolean;
  recurrencePolicy?: string;
}) {
  const { loading, unlocked, meta } = useCapability('cart.lineItems');
  const { open } = useUnlockDialog();
  const { addItem, addRecurring } = useCart();
  const [state, setState] = useState<'idle' | 'adding' | 'added'>('idle');
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
    if (state === 'adding') return;
    setState('adding');
    try {
      const resolved = await resolveSku();
      if (resolved) {
        if (recurring) {
          await addRecurring(resolved, recurrencePolicy);
        } else {
          await addItem(resolved, 1);
        }
        setState('added');
        setTimeout(() => setState('idle'), 1500);
      } else {
        setState('idle');
      }
    } catch {
      setState('idle');
    }
  }

  const idleLabel = recurring ? 'Subscribe & Save' : 'Add to cart';
  const label = state === 'adding' ? 'Adding…' : state === 'added' ? 'Added ✓' : idleLabel;
  return (
    <button
      onClick={onClick}
      disabled={state === 'adding'}
      className={`rounded-lg px-4 py-2.5 text-sm font-medium text-white hover:bg-[var(--color-terra-dark)] disabled:opacity-70 ${
        full ? 'w-full' : ''
      } ${unlocked ? 'bg-[var(--color-terra)]' : 'bg-[var(--color-terra)]/50'}`}
    >
      {unlocked ? label : (<><Lock size={13} className="mr-1 inline align-[-2px]" />Add to cart</>)}
    </button>
  );
}
