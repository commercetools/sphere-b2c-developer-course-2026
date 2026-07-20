/** Product/category view models — exactly the shapes the BFF catalog endpoints return. */
export interface MoneyView {
  currencyCode: string;
  centAmount: number;
}

export interface ProductView {
  key: string;
  name: string;
  slug: string;
  price: MoneyView | null;
  imageUrl?: string | null;
}

export interface CategoryView {
  key: string;
  name: string;
  slug: string;
  children?: CategoryView[];
}
