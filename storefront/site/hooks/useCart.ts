'use client';

import useSWR from 'swr';
import { bffGet, bffPost, bffPatch, bffDelete, BffResult } from '@/lib/bff/client';
import { usePreferences } from '@/context/preferences-context';
import { CartView } from '@/lib/cart-types';

const KEY = 'cart';

/**
 * The one cart data hook. Reads GET /api/cart (through the BFF proxy) and exposes the write actions,
 * each of which refreshes the cached cart on success. The BFF owns the active guest cart + its version
 * (fetch-before-update + 409 retry), so the storefront just states intent; it only makes sure a cart
 * exists before the first mutation (POST /api/cart, seeded with the shopper's currency/country).
 */
export function useCart() {
  const { currency, country } = usePreferences();
  const { data, mutate, isLoading } = useSWR(KEY, (p) => bffGet<CartView>(p), {
    revalidateOnFocus: true,
  });
  const cart = data && data.ok ? data.data : null;

  async function ensureCart(): Promise<void> {
    if (cart?.id) return;
    const qs = new URLSearchParams();
    if (currency) qs.set('currency', currency);
    if (country) qs.set('country', country);
    await bffPost<CartView>(`cart?${qs.toString()}`);
  }

  async function run(op: () => Promise<BffResult<CartView>>): Promise<BffResult<CartView>> {
    const res = await op();
    await mutate();
    return res;
  }

  return {
    cart,
    loading: isLoading,
    notImplemented: data?.notImplemented ?? false,
    refresh: () => mutate(),

    async addItem(sku: string, quantity = 1) {
      await ensureCart();
      return run(() => bffPost<CartView>('cart/line-items', { sku, quantity }));
    },
    async addRecurring(sku: string, policyKey = 'monthly') {
      await ensureCart();
      const qs = new URLSearchParams({ recurring: 'true', recurrencePolicy: policyKey });
      return run(() => bffPost<CartView>(`cart/line-items?${qs.toString()}`, { sku }));
    },
    async addBundle(bundleKey: string) {
      await ensureCart();
      return run(() => bffPost<CartView>('cart/bundles', { bundleKey }));
    },
    async changeQuantity(lineItemId: string, quantity: number) {
      return run(() => bffPatch<CartView>(`cart/line-items/${lineItemId}`, { quantity }));
    },
    async removeLine(lineItemId: string) {
      return run(() => bffDelete<CartView>(`cart/line-items/${lineItemId}`));
    },
    async applyCode(code: string) {
      return run(() => bffPost<CartView>('cart/discount-codes', { code }));
    },
  };
}
