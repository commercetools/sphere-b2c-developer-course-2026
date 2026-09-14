package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.CustomerAddress;

public record AddressView(
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

    public static AddressView from(CustomerAddress a) {
        return new AddressView(a.id(), a.key(), a.country(), a.firstName(), a.lastName(),
                a.streetName(), a.streetNumber(), a.postalCode(), a.city(), a.defaultShipping(), a.defaultBilling());
    }
}
