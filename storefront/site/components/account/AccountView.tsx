'use client';

import { FeatureGate } from '@/components/ui/FeatureGate';

/** Account. Gated on customer.identity; a sample sign-in card shows dimmed under the Unlock overlay. */
export function AccountView() {
  return (
    <div className="mx-auto max-w-md space-y-6">
      <FeatureGate capability="customer.identity" title="Login / register / account">
        <div className="space-y-4 rounded-2xl border border-[var(--color-border)] bg-white p-6">
          <h2 className="font-display text-xl">Sign in</h2>
          <input placeholder="Email" className="w-full rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm" />
          <input
            placeholder="Password"
            type="password"
            className="w-full rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm"
          />
          <button className="w-full rounded-lg bg-[var(--color-terra)] px-5 py-2.5 font-medium text-white">
            Sign in
          </button>
          <p className="text-center text-sm text-[var(--color-charcoal-light)]">
            New here? <span className="text-[var(--color-terra)]">Create an account</span>
          </p>
        </div>
      </FeatureGate>

      <FeatureGate capability="customer.cartMerge" title="Cart merge on sign-in">
        <div className="rounded-2xl border border-[var(--color-border)] bg-white p-4 text-sm text-[var(--color-charcoal-light)]">
          Your anonymous cart merges into your account cart when you sign in.
        </div>
      </FeatureGate>
    </div>
  );
}
