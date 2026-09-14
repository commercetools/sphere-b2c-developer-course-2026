package com.lifestylehomecorp.customer.domain;

/**
 * What the storefront may know about the session (5.9): signed in or not, a first name to greet with,
 * and the pricing tier — nothing more. No ids cross to the browser.
 */
public record ShopperContext(boolean signedIn, String firstName, String customerGroupKey) {

    public static ShopperContext guest() {
        return new ShopperContext(false, null, null);
    }
}
