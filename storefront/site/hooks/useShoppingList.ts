'use client';

import useSWR, { mutate as globalMutate } from 'swr';
import { bffGet, bffPost, bffDelete } from '@/lib/bff/client';
import { SavedListView } from '@/lib/shopping-list-types';
import { CartView } from '@/lib/cart-types';

const KEY = 'shopping-list';

/**
 * The guest wishlist (cart.shoppingList). Reads GET /api/shopping-list and exposes the write actions.
 * Actions that move items between the list and the cart refresh BOTH SWR caches (the list and the
 * shared "cart" key), so the cart panel + header badge update together.
 */
export function useShoppingList() {
  const { data, mutate, isLoading } = useSWR(KEY, (p) => bffGet<SavedListView>(p), {
    revalidateOnFocus: true,
  });
  const list = data && data.ok ? data.data : null;

  const refreshList = () => mutate();
  const refreshBoth = () => Promise.all([mutate(), globalMutate('cart')]);

  return {
    list,
    loading: isLoading,
    notImplemented: data?.notImplemented ?? false,
    refresh: refreshList,

    /** ♡ Save a SKU for later (keeps it in the wishlist). */
    async save(sku: string, quantity = 1) {
      const r = await bffPost<SavedListView>('shopping-list', { sku, quantity });
      await refreshList();
      return r;
    },
    /** Remove a saved item outright. */
    async removeItem(lineItemId: string) {
      const r = await bffDelete<SavedListView>(`shopping-list/items/${lineItemId}`);
      await refreshList();
      return r;
    },
    /** Move one saved item into the cart (add it, remove it from the list). */
    async moveToCart(lineItemId: string) {
      const r = await bffPost<CartView>(`shopping-list/items/${lineItemId}/to-cart`);
      await refreshBoth();
      return r;
    },
    /** Move every saved item into the cart, then empty the list. */
    async moveAllToCart() {
      const r = await bffPost<CartView>('shopping-list/to-cart');
      await refreshBoth();
      return r;
    },
    /** Move a cart line to the list (add here, remove from the cart). */
    async saveForLater(cartLineItemId: string) {
      const r = await bffPost<SavedListView>('shopping-list/save-for-later', { lineItemId: cartLineItemId });
      await refreshBoth();
      return r;
    },
  };
}
