/** The guest wishlist / saved-items contract as returned by the BFF (GET /api/shopping-list). */
export interface SavedItemView {
  lineItemId: string;
  sku: string | null;
  name: string;
  quantity: number;
}

export interface SavedListView {
  id: string | null;
  name: string;
  items: SavedItemView[];
  itemCount: number;
}
