/**
 * The cart contract as returned by the BFF (GET /api/cart and every cart mutation). A bundle comes
 * back as ONE parent line with its components in `children` (money rolled up onto the parent), so
 * `lines.length === itemCount` and the storefront can render the group straight through.
 */
export interface MoneyView {
  centAmount: number;
  currencyCode: string;
  fractionDigits: number;
}

export interface CartLineView {
  lineItemId: string;
  sku: string | null;
  name: string;
  quantity: number;
  unitPrice: MoneyView | null;
  lineTotal: MoneyView | null;
  lineSavings: MoneyView | null;
  /** Set on a bundle parent — its identity. */
  bundleId: string | null;
  /** Set on a bundle child — equals the parent's bundleId. */
  parentKey: string | null;
  recurring: boolean;
  /** e.g. "Every month" — a recurring line's frequency (null when not recurring). */
  recurrenceLabel: string | null;
  /** "Fixed" (price locked at subscribe time) or "Dynamic" (re-priced each cycle); null when not recurring. */
  recurrencePriceMode: string | null;
  distributionChannelId: string | null;
  supplyChannelId: string | null;
  inventoryMode: string | null;
  /** A bundle parent's component lines; empty for ordinary lines and for children. */
  children: CartLineView[];
}

export interface CartView {
  id: string | null;
  version: number | null;
  currency: string | null;
  country: string | null;
  lines: CartLineView[];
  itemCount: number;
  subtotal: MoneyView | null;
  savings: MoneyView | null;
  shipping: MoneyView | null;
  tax: MoneyView | null;
  total: MoneyView | null;
  shippingMethod: string | null;
  discountCodes: string[];
}
