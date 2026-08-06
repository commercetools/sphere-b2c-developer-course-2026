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
  /** Pre-discount list price when a discount applies (S3 search cards), else absent/null. */
  originalPrice?: MoneyView | null;
  imageUrl?: string | null;
}

export interface CategoryView {
  key: string;
  name: string;
  slug: string;
  children?: CategoryView[];
}

/** The Session-2 PLP envelope: this page's cards + `total` (all matching products) for "Total N products". */
export interface ProductPageView {
  products: ProductView[];
  total: number;
}

/**
 * Session-3 store-scoped search/PLP wire models — the shapes the product-discovery BFF returns
 * (CardView / PlpView / FacetView / FacetBucketView / PriceStatsView). camelCase JSON.
 */
export interface CardView {
  key: string;
  name: string;
  slug: string;
  price: MoneyView | null;
  originalPrice: MoneyView | null;
  imageUrl: string | null;
}

export interface FacetBucketView {
  key: string;
  count: number;
}

export interface PriceStatsView {
  min: number;
  max: number;
  mean: number;
}

export interface FacetView {
  name: string;
  /** "bucket" for distinct/ranges facets, "stats" for a numeric (price) stats facet. */
  type: 'bucket' | 'stats';
  buckets: FacetBucketView[] | null;
  stats: PriceStatsView | null;
}

/** The composed PLP / faceted-search envelope: cards, facets and the paging window. */
export interface PlpView {
  cards: CardView[];
  facets: FacetView[];
  total: number;
  offset: number;
  limit: number;
}
