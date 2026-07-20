import { CategoryView, ProductView } from './types';

/**
 * Sample content shown under a LOCKED feature so the storefront always reads like a real shop
 * (dimmed, with an Unlock button). Once the feature unlocks, live BFF data replaces this.
 */
const eur = (centAmount: number) => ({ currencyCode: 'EUR', centAmount });

export const SAMPLE_CATEGORIES: CategoryView[] = [
  { key: 'living', name: 'Living Room', slug: 'living-room' },
  { key: 'bedroom', name: 'Bedroom', slug: 'bedroom' },
  { key: 'lighting', name: 'Lighting', slug: 'lighting' },
  { key: 'decor', name: 'Décor', slug: 'decor' },
];

export const SAMPLE_PRODUCTS: ProductView[] = [
  { key: 'linen-sofa', name: 'Linen Two-Seater Sofa', slug: 'linen-sofa', price: eur(89900) },
  { key: 'oak-table', name: 'Solid Oak Dining Table', slug: 'oak-table', price: eur(124000) },
  { key: 'ceramic-lamp', name: 'Ceramic Table Lamp', slug: 'ceramic-lamp', price: eur(8900) },
  { key: 'wool-throw', name: 'Merino Wool Throw', slug: 'wool-throw', price: eur(5900) },
  { key: 'stoneware-vase', name: 'Stoneware Vase', slug: 'stoneware-vase', price: eur(3400) },
  { key: 'walnut-shelf', name: 'Walnut Wall Shelf', slug: 'walnut-shelf', price: eur(21000) },
  { key: 'cotton-bedding', name: 'Cotton Bedding Set', slug: 'cotton-bedding', price: eur(12000) },
  { key: 'rattan-chair', name: 'Rattan Armchair', slug: 'rattan-chair', price: eur(34000) },
];

export const SAMPLE_PRODUCT: ProductView = SAMPLE_PRODUCTS[0];
