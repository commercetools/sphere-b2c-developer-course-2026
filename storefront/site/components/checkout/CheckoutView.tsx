'use client';

import { FeatureGate } from '@/components/ui/FeatureGate';

/** Checkout. Gated on order.create; a sample checkout form shows dimmed under the Unlock overlay. */
export function CheckoutView() {
  return (
    <FeatureGate capability="order.create" title="Checkout & order placement">
      <div className="grid gap-6 md:grid-cols-2">
        <div className="space-y-4 rounded-2xl border border-[var(--color-border)] bg-white p-6">
          <h2 className="font-medium">Shipping address</h2>
          {['Full name', 'Address', 'City', 'Postal code'].map((f) => (
            <input
              key={f}
              placeholder={f}
              className="w-full rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm"
            />
          ))}
          <h2 className="pt-2 font-medium">Payment</h2>
          <input
            placeholder="Card number"
            className="w-full rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm"
          />
        </div>
        <div className="space-y-4 rounded-2xl border border-[var(--color-border)] bg-white p-6">
          <h2 className="font-medium">Order summary</h2>
          <div className="flex justify-between text-sm">
            <span className="text-[var(--color-charcoal-light)]">Total</span>
            <span className="font-semibold">€1,077.00</span>
          </div>
          <button className="w-full rounded-lg bg-[var(--color-terra)] px-5 py-3 font-medium text-white">
            Place order
          </button>
        </div>
      </div>
    </FeatureGate>
  );
}
