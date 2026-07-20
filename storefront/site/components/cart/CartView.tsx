'use client';

import { FeatureGate } from '@/components/ui/FeatureGate';
import { Package } from '@/components/ui/icons';

const SAMPLE_LINES = [
  { name: 'Linen Two-Seater Sofa', qty: 1, price: '€899.00' },
  { name: 'Ceramic Table Lamp', qty: 2, price: '€178.00' },
];

/** Cart. Gated on cart.write; a sample cart shows dimmed under the Unlock overlay when locked. */
export function CartView() {
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <FeatureGate capability="cart.write" title="Cart">
        <div className="divide-y divide-[var(--color-border)] rounded-2xl border border-[var(--color-border)] bg-white">
          {SAMPLE_LINES.map((l) => (
            <div key={l.name} className="flex items-center gap-4 p-4">
              <div className="grid h-16 w-16 place-items-center rounded-lg bg-[var(--color-cream-dark)] text-[var(--color-charcoal-light)]">
                <Package size={26} />
              </div>
              <div className="flex-1">
                <p className="font-medium">{l.name}</p>
                <p className="text-sm text-[var(--color-charcoal-light)]">Qty {l.qty}</p>
              </div>
              <span className="font-medium">{l.price}</span>
            </div>
          ))}
        </div>
      </FeatureGate>

      <div className="space-y-4">
        <div className="rounded-2xl border border-[var(--color-border)] bg-white p-5">
          <div className="flex justify-between text-sm">
            <span className="text-[var(--color-charcoal-light)]">Subtotal</span>
            <span className="font-medium">€1,077.00</span>
          </div>
          <div className="mt-1 flex justify-between text-sm">
            <span className="text-[var(--color-charcoal-light)]">Delivery</span>
            <span className="font-medium">Free</span>
          </div>
        </div>
        <FeatureGate capability="order.create" title="Checkout">
          <a
            href="checkout"
            className="block rounded-lg bg-[var(--color-terra)] px-5 py-3 text-center font-medium text-white hover:bg-[var(--color-terra-dark)]"
          >
            Proceed to checkout
          </a>
        </FeatureGate>
      </div>
    </div>
  );
}
