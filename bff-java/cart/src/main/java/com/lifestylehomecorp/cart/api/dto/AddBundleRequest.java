package com.lifestylehomecorp.cart.api.dto;

/** Request body for POST /api/cart/bundles (4.3) — the bundle product's key (e.g. {@code bedding-bundle}). */
public record AddBundleRequest(String bundleKey) {
}
