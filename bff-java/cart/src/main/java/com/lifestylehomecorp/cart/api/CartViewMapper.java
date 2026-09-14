package com.lifestylehomecorp.cart.api;

import com.lifestylehomecorp.cart.api.dto.CartLineView;
import com.lifestylehomecorp.cart.api.dto.CartView;
import com.lifestylehomecorp.cart.api.dto.MoneyView;
import com.lifestylehomecorp.cart.domain.CartLine;
import com.lifestylehomecorp.cart.domain.CartSummary;
import com.lifestylehomecorp.cart.domain.Money;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps the domain {@link CartSummary} (a FLAT list of lines, mirroring commercetools) to the wire
 * {@link CartView}, nesting each bundle's children under their parent line. The domain stays flat and
 * honest; the presentation shape (parent + indented children, one rolled-up total) is built here.
 */
public final class CartViewMapper {

    private CartViewMapper() {
    }

    public static CartView toView(CartSummary c) {
        // Group children (lines carrying a parentKey) by the bundleId they point at.
        Map<String, List<CartLine>> childrenByBundle = new LinkedHashMap<>();
        for (CartLine l : c.lines()) {
            if (l.parentKey() != null) {
                childrenByBundle.computeIfAbsent(l.parentKey(), k -> new ArrayList<>()).add(l);
            }
        }
        // Top-level lines = ordinary lines + bundle parents (children nested); children are folded in.
        List<CartLineView> lines = new ArrayList<>();
        for (CartLine l : c.lines()) {
            if (l.parentKey() != null) {
                continue; // a bundle child — rendered inside its parent, not at top level
            }
            List<CartLine> kids = l.bundleId() != null
                    ? childrenByBundle.getOrDefault(l.bundleId(), List.of()) : List.of();
            lines.add(kids.isEmpty() ? CartLineView.from(l) : bundleParent(l, kids, c.currency()));
        }

        return new CartView(
                c.id(), c.version(), c.currency(), c.country(),
                lines,
                c.itemCount(),
                MoneyView.from(c.subtotal()),
                MoneyView.from(c.savings()),
                MoneyView.from(c.shipping()),
                MoneyView.from(c.tax()),
                MoneyView.from(c.total()),
                c.shippingMethod(),
                c.discountCodes());
    }

    /** Build a bundle parent view: children nested, money rolled up from them (the parent is priced 0). */
    private static CartLineView bundleParent(CartLine parent, List<CartLine> kids, String currency) {
        List<CartLineView> children = kids.stream().map(CartLineView::from).toList();
        long total = kids.stream().mapToLong(k -> amount(k.lineTotal())).sum();
        long savings = kids.stream().mapToLong(k -> amount(k.lineSavings())).sum();
        int fractionDigits = kids.stream()
                .map(CartLine::lineTotal).filter(m -> m != null)
                .map(Money::fractionDigits)
                .findFirst().orElse(2);
        MoneyView rolledTotal = new MoneyView(total, currency, fractionDigits);
        MoneyView rolledSavings = new MoneyView(savings, currency, fractionDigits);
        return CartLineView.bundleParent(parent, rolledTotal, rolledSavings, children);
    }

    private static long amount(Money m) {
        return m == null ? 0 : m.centAmount();
    }
}
