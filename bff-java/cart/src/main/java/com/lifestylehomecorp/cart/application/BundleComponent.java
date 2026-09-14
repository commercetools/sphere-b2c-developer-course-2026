package com.lifestylehomecorp.cart.application;

/** One resolved bundle component: the child SKU to add as a line item, and how many. */
public record BundleComponent(String sku, long quantity) {
}
