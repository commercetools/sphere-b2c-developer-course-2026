'use client';

import { FeatureGate } from '@/components/ui/FeatureGate';

const input = 'w-full rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm';
const card = 'space-y-4 rounded-2xl border border-[var(--color-border)] bg-white p-6';
const primary = 'w-full rounded-lg bg-[var(--color-terra)] px-5 py-2.5 font-medium text-white';
const secondary = 'rounded-lg border border-[var(--color-border)] px-4 py-2 text-sm font-medium hover:border-[var(--color-terra)]';

/**
 * Account — the Session 5 surface. One FeatureGate per customer.* capability, so each task lights up
 * its own card as its BFF endpoint comes alive: sign in / register (5.2 / 5.1), the merge notice (5.3),
 * the profile (5.4), the address book (5.5), the pricing tier (5.6), password (5.7), email verification
 * (5.8), the session banner (5.9) and account deletion (5.10). Sample content shows dimmed until then.
 */
export function AccountView() {
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <FeatureGate capability="customer.session" title="Signed-in state">
        <div className="flex items-center justify-between rounded-2xl border border-[var(--color-border)] bg-white px-6 py-4 text-sm">
          <span>
            Signed in as <strong>Ada</strong> · pricing tier <strong>vip</strong>
          </span>
          <button className={secondary}>Sign out</button>
        </div>
      </FeatureGate>

      <div className="grid gap-6 md:grid-cols-2">
        <FeatureGate capability="customer.login" title="Sign in">
          <div className={card}>
            <h2 className="font-display text-xl">Sign in</h2>
            <input placeholder="Email" className={input} />
            <input placeholder="Password" type="password" className={input} />
            <button className={primary}>Sign in</button>
            <FeatureGate capability="customer.password" title="Forgot password">
              <p className="text-center text-sm text-[var(--color-terra)]">Forgot password?</p>
            </FeatureGate>
          </div>
        </FeatureGate>

        <FeatureGate capability="customer.register" title="Create an account">
          <div className={card}>
            <h2 className="font-display text-xl">Create an account</h2>
            <div className="grid grid-cols-2 gap-3">
              <input placeholder="First name" className={input} />
              <input placeholder="Last name" className={input} />
            </div>
            <input placeholder="Email" className={input} />
            <input placeholder="Password" type="password" className={input} />
            <button className={primary}>Create account</button>
            <p className="text-center text-xs text-[var(--color-charcoal-light)]">
              Your basket and saved items come with you.
            </p>
          </div>
        </FeatureGate>
      </div>

      <FeatureGate capability="customer.cartMerge" title="Cart merge on sign-in">
        <div className="rounded-2xl border border-[var(--color-border)] bg-white p-4 text-sm text-[var(--color-charcoal-light)]">
          We combined your basket: <strong>2 items</strong> added to the cart you already had, <strong>1 item</strong> merged
          (larger quantity kept).
        </div>
      </FeatureGate>

      <FeatureGate capability="customer.emailVerify" title="Verify your email">
        <div className="flex items-center justify-between rounded-2xl border border-amber-200 bg-amber-50 px-6 py-3 text-sm">
          <span>Please verify your email address.</span>
          <button className={secondary}>Resend link</button>
        </div>
      </FeatureGate>

      <div className="grid gap-6 md:grid-cols-2">
        <FeatureGate capability="customer.profile" title="My profile">
          <div className={card}>
            <h2 className="font-display text-xl">My profile</h2>
            <div className="grid grid-cols-2 gap-3">
              <input defaultValue="Ada" className={input} />
              <input defaultValue="Lovelace" className={input} />
            </div>
            <input defaultValue="ada@lhc.test" className={input} />
            <button className={secondary}>Save changes</button>
          </div>
        </FeatureGate>

        <FeatureGate capability="customer.groups" title="Your pricing tier">
          <div className={card}>
            <h2 className="font-display text-xl">Your pricing</h2>
            <p className="text-sm text-[var(--color-charcoal-light)]">
              You are in the <strong>vip</strong> group — the catalogue shows your member prices.
            </p>
            <p className="text-xs text-[var(--color-charcoal-light)]">Price context: USD · US · vip</p>
          </div>
        </FeatureGate>
      </div>

      <FeatureGate capability="customer.addresses" title="Address book">
        <div className={card}>
          <div className="flex items-center justify-between">
            <h2 className="font-display text-xl">Address book</h2>
            <button className={secondary}>Add address</button>
          </div>
          <ul className="divide-y divide-[var(--color-border)] text-sm">
            <li className="flex items-center justify-between py-3">
              <span>Ada Lovelace · Hauptstraße 1 · 10115 Berlin · DE</span>
              <span className="rounded-full bg-[var(--color-cream)] px-2 py-0.5 text-xs">Default shipping</span>
            </li>
            <li className="flex items-center justify-between py-3">
              <span>Ada Lovelace · 12 Regent St · London SW1Y · GB</span>
              <button className="text-xs text-[var(--color-terra)]">Make default</button>
            </li>
          </ul>
        </div>
      </FeatureGate>

      <div className="grid gap-6 md:grid-cols-2">
        <FeatureGate capability="customer.password" title="Change password">
          <div className={card}>
            <h2 className="font-display text-xl">Change password</h2>
            <input placeholder="Current password" type="password" className={input} />
            <input placeholder="New password" type="password" className={input} />
            <button className={secondary}>Update password</button>
          </div>
        </FeatureGate>

        <FeatureGate capability="customer.pii" title="Delete my account">
          <div className={card}>
            <h2 className="font-display text-xl">Delete my account</h2>
            <p className="text-sm text-[var(--color-charcoal-light)]">
              Removes your profile and addresses. Past orders are kept for accounting.
            </p>
            <button className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50">
              Delete account
            </button>
          </div>
        </FeatureGate>
      </div>
    </div>
  );
}
