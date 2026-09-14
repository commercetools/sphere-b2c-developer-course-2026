package com.lifestylehomecorp.cart.api.dto;

/** Body for PUT /api/cart/shipping-method (4.8) — the chosen method's id (from the matching list). */
public record SetShippingMethodRequest(String shippingMethodId) {
}
