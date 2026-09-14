package com.lifestylehomecorp.customer.api.dto;

/** Body for POST /api/customers/me/addresses. {@code key} is optional (one is minted); the defaults are opt-in. */
public record AddressRequest(
        String key,
        String country,
        String firstName,
        String lastName,
        String streetName,
        String streetNumber,
        String postalCode,
        String city,
        Boolean defaultShipping,
        Boolean defaultBilling) {
}
