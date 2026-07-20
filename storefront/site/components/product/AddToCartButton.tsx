'use client';

import { useCapability } from '@/context/capabilities-context';
import { useUnlockDialog } from '@/context/unlock-dialog';
import { Lock } from '@/components/ui/icons';

/** Add-to-cart, gated on cart.write. When locked it renders dimmed and opens the unlock dialog. */
export function AddToCartButton({ full = false }: { full?: boolean }) {
  const { loading, unlocked, meta } = useCapability('cart.write');
  const { open } = useUnlockDialog();
  if (loading) return null;
  return (
    <button
      onClick={unlocked ? undefined : () => open(meta)}
      className={`rounded-lg px-4 py-2.5 text-sm font-medium text-white hover:bg-[var(--color-terra-dark)] ${
        full ? 'w-full' : ''
      } ${unlocked ? 'bg-[var(--color-terra)]' : 'bg-[var(--color-terra)]/50'}`}
    >
      {unlocked ? 'Add to cart' : (<><Lock size={13} className="mr-1 inline align-[-2px]" />Add to cart</>)}
    </button>
  );
}
