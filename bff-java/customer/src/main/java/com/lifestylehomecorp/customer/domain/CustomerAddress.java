package com.lifestylehomecorp.customer.domain;

/** SDK-free address-book entry. {@code id} is server-assigned on add; {@code key} is the client's stable handle. */
public record CustomerAddress(
        String id,
        String key,
        String country,
        String firstName,
        String lastName,
        String streetName,
        String streetNumber,
        String postalCode,
        String city,
        boolean defaultShipping,
        boolean defaultBilling) {
}
