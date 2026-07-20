import { CartView } from '@/components/cart/CartView';

export default function CartPage() {
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Cart</h1>
      <CartView />
    </div>
  );
}
