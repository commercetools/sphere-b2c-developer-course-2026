package com.lifestylehomecorp.cart.domain;

import java.util.List;

/**
 * The storefront-facing cart, SDK-free — the shape returned by every cart endpoint (create, each
 * mutation, and GET /api/cart). Carries the money breakdown for the order summary (4.10):
 * subtotal, savings, shipping, tax, total. {@code itemCount} counts a bundle as one (children excluded).
 */
public record CartSummary(
        String id,
        Long version,
        String currency,
        String country,
        List<CartLine> lines,
        int itemCount,
        Money subtotal,
        Money savings,
        Money shipping,
        Money tax,
        Money total,
        String shippingMethod,
        List<String> discountCodes) {

    /** An empty cart (no cart created yet) — GET /api/cart never throws. */
    public static CartSummary empty() {
        return new CartSummary(null, null, null, null, List.of(), 0,
                null, null, null, null, null, null, List.of());
    }
}
