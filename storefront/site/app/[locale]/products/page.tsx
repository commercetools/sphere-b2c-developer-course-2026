import { CategoryNav } from '@/components/product/CategoryNav';
import { PlpSwitch } from '@/components/product/PlpSwitch';

export default function ProductsPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Products</h1>
      <CategoryNav />
      {/* One PLP, capability-switched: Session-2 grid until search.plpV2 unlocks, then the composed
          store-scoped search PLP. See PlpSwitch. */}
      <PlpSwitch />
    </div>
  );
}
