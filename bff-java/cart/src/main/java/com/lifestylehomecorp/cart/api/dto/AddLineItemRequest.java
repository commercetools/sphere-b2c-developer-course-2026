package com.lifestylehomecorp.cart.api.dto;

/** Body for POST /api/cart/line-items — the storefront says "add SKU × qty" (default qty 1). */
public record AddLineItemRequest(String sku, Long quantity) {
}
