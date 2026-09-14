package com.lifestylehomecorp.customer.api.dto;

import com.lifestylehomecorp.customer.domain.CustomerProfile;

import java.util.List;

/** The account page's customer — the PII-safe projection, nothing the page doesn't need (5.10). */
public record CustomerView(
        String id,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified,
        String customerGroupKey,
        List<String> stores,
        List<AddressView> addresses) {

    public static CustomerView from(CustomerProfile p) {
        return new CustomerView(p.id(), p.email(), p.firstName(), p.lastName(), p.emailVerified(),
                p.customerGroupKey(), p.storeKeys(), p.addresses().stream().map(AddressView::from).toList());
    }
}
