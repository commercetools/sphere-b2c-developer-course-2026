package com.lifestylehomecorp.cart.domain;

/** One saved (wishlist) line, SDK-free: enough to render it and to re-add it to the cart by SKU. */
public record SavedItem(String lineItemId, String sku, String name, long quantity) {
}
