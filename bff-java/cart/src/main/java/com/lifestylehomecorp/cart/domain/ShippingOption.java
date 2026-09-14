package com.lifestylehomecorp.cart.domain;

/** A shipping method valid for the cart (4.8) — SDK-free. */
public record ShippingOption(String id, String key, String name, boolean isDefault) {
}
