package com.lifestylehomecorp.cart.application;

/**
 * Inputs for creating a guest cart (4.1): the price-selection currency + country, the store scope,
 * and the anonymous identity (null = use the session's). SDK-free — the repository turns this into a {@code CartDraft}.
 */
public record CartContext(String currency, String country, String storeKey, String anonymousId) {
}
