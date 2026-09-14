package com.lifestylehomecorp.cart.application;

/** SDK-free shipping-address input (4.7). {@code country} is required; the rest are optional. */
public record AddressInput(
        String country,
        String firstName,
        String lastName,
        String streetName,
        String streetNumber,
        String postalCode,
        String city) {
}
