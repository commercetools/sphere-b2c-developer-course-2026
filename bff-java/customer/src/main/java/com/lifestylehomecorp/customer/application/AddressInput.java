package com.lifestylehomecorp.customer.application;

/** SDK-free address input (5.5). {@code key} is the client's stable handle — needed to make it default in the same update. */
public record AddressInput(
        String key,
        String country,
        String firstName,
        String lastName,
        String streetName,
        String streetNumber,
        String postalCode,
        String city) {
}
