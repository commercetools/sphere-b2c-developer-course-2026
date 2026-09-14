package com.lifestylehomecorp.cart.api.dto;

import java.util.List;

public record CartView(
        String id,
        Long version,
        String currency,
        String country,
        List<CartLineView> lines,
        int itemCount,
        MoneyView subtotal,
        MoneyView savings,
        MoneyView shipping,
        MoneyView tax,
        MoneyView total,
        String shippingMethod,
        List<String> discountCodes) {
}
