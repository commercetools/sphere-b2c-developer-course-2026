package com.lifestylehomecorp.cart.api.dto;

/** Body for PATCH /api/cart/line-items/{id} — new quantity (0 removes the line). */
public record ChangeQuantityRequest(long quantity) {
}
