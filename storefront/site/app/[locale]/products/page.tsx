import { CategoryNav } from '@/components/product/CategoryNav';
import { ProductGrid } from '@/components/product/ProductGrid';
import { FeatureGate } from '@/components/ui/FeatureGate';

export default function ProductsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Products</h1>
      <CategoryNav />
      <FeatureGate capability="catalog.plp" title="Product listing">
        <ProductGrid />
      </FeatureGate>
    </div>
  );
}
