package com.lifestylehomecorp.cart.api.dto;

/**
 * Body for PUT /api/cart/shipping-address (4.7). {@code country} is required (it drives shipping
 * matching + tax); the rest are optional address fields.
 */
public record ShippingAddressRequest(
        String country,
        String firstName,
        String lastName,
        String streetName,
        String streetNumber,
        String postalCode,
        String city) {
}
