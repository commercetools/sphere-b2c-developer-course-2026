'use client';

import { useState } from 'react';
import { useLocale } from 'next-intl';
import { FeatureGate } from '@/components/ui/FeatureGate';
import { useCapability } from '@/context/capabilities-context';
import { useCart } from '@/hooks/useCart';
import { useShoppingList } from '@/hooks/useShoppingList';
import { CartLineView, MoneyView } from '@/lib/cart-types';
import { SavedItemView } from '@/lib/shopping-list-types';
import { formatMoney } from '@/lib/utils';
import { Package, Layers, Sparkles, Tag, Heart } from '@/components/ui/icons';

const m = (centAmount: number): MoneyView => ({ centAmount, currencyCode: 'EUR', fractionDigits: 2 });
const line = (over: Partial<CartLineView> & { name: string; lineItemId: string }): CartLineView => ({
  sku: null, quantity: 1, unitPrice: null, lineTotal: null, lineSavings: m(0),
  bundleId: null, parentKey: null, recurring: false, recurrenceLabel: null, recurrencePriceMode: null,
  distributionChannelId: null, supplyChannelId: null, inventoryMode: null, children: [], ...over,
});

/** Shown (dimmed) while cart.summary is locked, so the cart still reads like a real shop — and it
 *  demonstrates the bundle grouping the live cart will render once unlocked. */
const SAMPLE_LINES: CartLineView[] = [
  line({ lineItemId: 's1', name: 'Linen Two-Seater Sofa', unitPrice: m(89900), lineTotal: m(89900) }),
  line({
    lineItemId: 's2', name: 'Bedding Bundle', bundleId: 'sample', unitPrice: m(3198), lineTotal: m(3198), lineSavings: m(500),
    children: [
      line({ lineItemId: 's2a', name: 'Luxe Pillow Cover', parentKey: 'sample', unitPrice: m(2599), lineTotal: m(2247) }),
      line({ lineItemId: 's2b', name: 'Lana Pillow Cover', parentKey: 'sample', unitPrice: m(1099), lineTotal: m(951) }),
    ],
  }),
];
const SAMPLE_TOTALS = { subtotal: m(93598), savings: m(500), shipping: null, tax: null, total: m(93098) };

export function CartView() {
  const locale = useLocale();
  const { unlocked } = useCapability('cart.summary');
  const { cart, changeQuantity, removeLine, applyCode } = useCart();
  const { saveForLater } = useShoppingList();
  const [code, setCode] = useState('');
  const [busy, setBusy] = useState(false);

  const fmt = (v: MoneyView | null | undefined) => (v ? formatMoney(v.centAmount, v.currencyCode, locale) : '—');

  // Locked → sample (dimmed by the gate). Unlocked → the real cart (empty state handled below).
  const live = unlocked && cart ? cart.lines : null;
  const lines = live ?? SAMPLE_LINES;
  const totals = unlocked && cart ? cart : SAMPLE_TOTALS;
  const empty = unlocked && (!cart || cart.lines.length === 0);

  const act = async (fn: () => Promise<unknown>) => {
    if (busy) return;
    setBusy(true);
    try { await fn(); } finally { setBusy(false); }
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
      <div className="space-y-6">
        <FeatureGate capability="cart.summary" title="Cart">
          <div className="rounded-2xl border border-[var(--color-border)] bg-white">
            {empty ? (
              <p className="p-8 text-center text-[var(--color-charcoal-light)]">
                Your cart is empty — add something from the catalogue.
              </p>
            ) : (
              <div className="divide-y divide-[var(--color-border)]">
                {lines.map((l) => (
                  <CartRow
                    key={l.lineItemId}
                    l={l}
                    fmt={fmt}
                    editable={!!live}
                    busy={busy}
                    onQty={(q) => act(() => changeQuantity(l.lineItemId, q))}
                    onRemove={() => act(() => removeLine(l.lineItemId))}
                    onSaveForLater={() => act(() => saveForLater(l.lineItemId))}
                  />
                ))}
              </div>
            )}
          </div>
        </FeatureGate>

        {/* Saved items / wishlist (cart.shoppingList) */}
        <SavedItemsPanel />
      </div>

      <div className="space-y-4">
        {/* Promo code (cart.promo) */}
        <FeatureGate capability="cart.promo" title="Promo code">
          <form
            className="rounded-2xl border border-[var(--color-border)] bg-white p-4"
            onSubmit={(e) => { e.preventDefault(); if (code.trim()) act(() => applyCode(code.trim())); }}
          >
            <label className="mb-2 flex items-center gap-2 text-sm font-medium">
              <Tag size={15} /> Promo code
            </label>
            <div className="flex gap-2">
              <input
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="e.g. SAVE10"
                className="flex-1 rounded-lg border border-[var(--color-border)] px-3 py-2 text-sm"
              />
              <button type="submit" disabled={busy}
                className="rounded-lg bg-[var(--color-charcoal)] px-4 py-2 text-sm font-medium text-white disabled:opacity-50">
                Apply
              </button>
            </div>
            {unlocked && cart?.discountCodes?.length ? (
              <p className="mt-2 text-xs text-[var(--color-sage)]">Applied: {cart.discountCodes.join(', ')}</p>
            ) : null}
          </form>
        </FeatureGate>

        {/* Order summary */}
        <div className="rounded-2xl border border-[var(--color-border)] bg-white p-5">
          <Row label="Subtotal" value={fmt(totals.subtotal)} />
          {totals.savings && totals.savings.centAmount > 0 ? (
            <Row label="Savings" value={`−${fmt(totals.savings)}`} accent />
          ) : null}
          <Row label="Delivery" value={totals.shipping ? fmt(totals.shipping) : 'Calculated at checkout'} />
          {totals.tax ? <Row label="Tax" value={fmt(totals.tax)} /> : null}
          <div className="mt-2 border-t border-[var(--color-border)] pt-2">
            <Row label="Total" value={fmt(totals.total)} strong />
          </div>
        </div>

        <FeatureGate capability="order.create" title="Checkout">
          <a href="checkout"
            className="block rounded-lg bg-[var(--color-terra)] px-5 py-3 text-center font-medium text-white hover:bg-[var(--color-terra-dark)]">
            Proceed to checkout
          </a>
        </FeatureGate>
      </div>
    </div>
  );
}

/** One cart line. A bundle parent renders its children indented beneath it + a "Bundle saving" line. */
function CartRow({
  l, fmt, editable, busy, onQty, onRemove, onSaveForLater,
}: {
  l: CartLineView;
  fmt: (v: MoneyView | null | undefined) => string;
  editable: boolean;
  busy: boolean;
  onQty: (quantity: number) => void;
  onRemove: () => void;
  onSaveForLater?: () => void;
}) {
  const isBundle = l.children.length > 0;
  return (
    <div className="p-4">
      <div className="flex items-center gap-4">
        <div className="grid h-16 w-16 place-items-center rounded-lg bg-[var(--color-cream-dark)] text-[var(--color-charcoal-light)]">
          {isBundle ? <Layers size={24} /> : <Package size={24} />}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-medium">{l.name}</p>
            {isBundle ? <Badge>Bundle</Badge> : null}
            {l.recurring ? (
              <Badge icon>
                <Sparkles size={11} /> Subscribe &amp; Save{l.recurrenceLabel ? ` · ${l.recurrenceLabel}` : ''}
              </Badge>
            ) : null}
          </div>
          {l.recurring && l.recurrencePriceMode ? (
            <p className="mt-0.5 text-xs text-[var(--color-charcoal-light)]">
              {l.recurrencePriceMode === 'Dynamic' ? 'Price updates each delivery' : 'Price locked at today’s rate'}
            </p>
          ) : null}
          {editable && !isBundle ? (
            <QtyStepper qty={l.quantity} busy={busy} onQty={onQty} />
          ) : (
            <p className="mt-0.5 text-sm text-[var(--color-charcoal-light)]">Qty {l.quantity}</p>
          )}
        </div>
        <div className="text-right">
          <p className="font-medium">{fmt(l.lineTotal)}</p>
          {editable ? (
            <div className="mt-1 flex flex-col items-end gap-0.5">
              {onSaveForLater && !isBundle ? (
                <button onClick={onSaveForLater} disabled={busy}
                  className="inline-flex items-center gap-1 text-xs text-[var(--color-charcoal-light)] underline hover:text-[var(--color-terra)] disabled:opacity-50">
                  <Heart size={11} /> Save for later
                </button>
              ) : null}
              <button onClick={onRemove} disabled={busy}
                className="text-xs text-[var(--color-charcoal-light)] underline hover:text-[var(--color-terra)] disabled:opacity-50">
                Remove
              </button>
            </div>
          ) : null}
        </div>
      </div>

      {isBundle ? (
        <div className="mt-3 space-y-1 border-l-2 border-[var(--color-border)] pl-4">
          {l.children.map((c) => (
            <div key={c.lineItemId} className="flex justify-between text-sm text-[var(--color-charcoal-light)]">
              <span>{c.quantity} × {c.name}</span>
              <span>{fmt(c.lineTotal)}</span>
            </div>
          ))}
          {l.lineSavings && l.lineSavings.centAmount > 0 ? (
            <div className="flex justify-between text-sm text-[var(--color-sage)]">
              <span>Bundle saving</span>
              <span>−{fmt(l.lineSavings)}</span>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function QtyStepper({ qty, busy, onQty }: { qty: number; busy: boolean; onQty: (q: number) => void }) {
  return (
    <div className="mt-1 inline-flex items-center rounded-lg border border-[var(--color-border)]">
      <button onClick={() => onQty(Math.max(1, qty - 1))} disabled={busy || qty <= 1}
        className="px-2.5 py-1 text-[var(--color-charcoal-light)] disabled:opacity-40" aria-label="Decrease quantity">−</button>
      <span className="min-w-[2ch] px-2 text-center text-sm">{qty}</span>
      <button onClick={() => onQty(qty + 1)} disabled={busy}
        className="px-2.5 py-1 text-[var(--color-charcoal-light)] disabled:opacity-40" aria-label="Increase quantity">+</button>
    </div>
  );
}

function Badge({ children, icon }: { children: React.ReactNode; icon?: boolean }) {
  return (
    <span className={`inline-flex items-center gap-1 rounded-full bg-[var(--color-cream-dark)] px-2 py-0.5 text-[11px] font-medium text-[var(--color-charcoal-light)] ${icon ? '' : ''}`}>
      {children}
    </span>
  );
}

function Row({ label, value, strong, accent }: { label: string; value: string; strong?: boolean; accent?: boolean }) {
  return (
    <div className="flex justify-between text-sm">
      <span className="text-[var(--color-charcoal-light)]">{label}</span>
      <span className={`${strong ? 'text-base font-semibold' : 'font-medium'} ${accent ? 'text-[var(--color-sage)]' : ''}`}>{value}</span>
    </div>
  );
}

const SAMPLE_SAVED: SavedItemView[] = [
  { lineItemId: 'w1', sku: null, name: 'Merino Wool Throw', quantity: 1 },
  { lineItemId: 'w2', sku: null, name: 'Stoneware Vase', quantity: 1 },
];

/** Saved-for-later / wishlist panel below the cart (cart.shoppingList). Move one or all back to the cart. */
function SavedItemsPanel() {
  const { unlocked } = useCapability('cart.shoppingList');
  const { list, moveToCart, moveAllToCart, removeItem } = useShoppingList();
  const [busy, setBusy] = useState(false);
  const act = async (fn: () => Promise<unknown>) => {
    if (busy) return;
    setBusy(true);
    try { await fn(); } finally { setBusy(false); }
  };

  const items = unlocked && list ? list.items : SAMPLE_SAVED;
  // Unlocked but nothing saved → hide the panel entirely (no clutter). Locked → sample under the gate.
  if (unlocked && items.length === 0) return null;

  return (
    <FeatureGate capability="cart.shoppingList" title="Saved items">
      <div className="rounded-2xl border border-[var(--color-border)] bg-white">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] p-4">
          <h2 className="flex items-center gap-2 font-medium"><Heart size={16} /> Saved for later</h2>
          {unlocked && items.length > 0 ? (
            <button onClick={() => act(() => moveAllToCart())} disabled={busy}
              className="text-xs font-medium text-[var(--color-terra)] underline hover:text-[var(--color-terra-dark)] disabled:opacity-50">
              Move all to cart
            </button>
          ) : null}
        </div>
        <div className="divide-y divide-[var(--color-border)]">
          {items.map((it) => (
            <div key={it.lineItemId} className="flex items-center gap-4 p-4">
              <div className="grid h-12 w-12 place-items-center rounded-lg bg-[var(--color-cream-dark)] text-[var(--color-charcoal-light)]">
                <Package size={20} />
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium">{it.name}</p>
                <p className="text-sm text-[var(--color-charcoal-light)]">Qty {it.quantity}</p>
              </div>
              {unlocked ? (
                <div className="flex items-center gap-3 text-xs">
                  <button onClick={() => act(() => moveToCart(it.lineItemId))} disabled={busy}
                    className="font-medium text-[var(--color-terra)] underline hover:text-[var(--color-terra-dark)] disabled:opacity-50">
                    Move to cart
                  </button>
                  <button onClick={() => act(() => removeItem(it.lineItemId))} disabled={busy}
                    className="text-[var(--color-charcoal-light)] underline hover:text-[var(--color-terra)] disabled:opacity-50">
                    Remove
                  </button>
                </div>
              ) : null}
            </div>
          ))}
        </div>
      </div>
    </FeatureGate>
  );
}
