package com.lifestylehomecorp.cart.api.dto;

/** Request body for POST /api/shopping-list — save a SKU for later ({@code quantity} defaults to 1). */
public record AddToListRequest(String sku, Long quantity) {
}
