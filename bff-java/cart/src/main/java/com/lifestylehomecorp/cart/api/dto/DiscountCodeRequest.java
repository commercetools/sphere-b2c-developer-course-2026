package com.lifestylehomecorp.cart.api.dto;

/** Request body for POST /api/cart/discount-codes (4.9) — the promo code to apply (e.g. {@code SAVE10}). */
public record DiscountCodeRequest(String code) {
}
