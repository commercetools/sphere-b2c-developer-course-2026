package com.lifestylehomecorp.customer.domain;

/**
 * The resolved price-selection tuple for the current shopper (5.6) — the same inputs the catalogue
 * reads apply, made visible so Canvas and the storefront can SHOW why a price changed after login.
 * The group is null for a guest: a guest has no group, and the ungrouped price is the guest price.
 */
public record PriceContext(
        String currency,
        String country,
        String channel,
        String customerGroupId,
        String customerGroupKey,
        boolean signedIn) {
}
