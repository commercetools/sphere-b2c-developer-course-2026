package com.lifestylehomecorp.customer.domain;

import java.util.List;

/**
 * The PII-safe projection of a commercetools Customer (5.10): exactly what the account page needs.
 * No password (the SDK type has none either), no customerNumber / externalId / custom fields.
 */
public record CustomerProfile(
        String id,
        String email,
        String firstName,
        String lastName,
        boolean emailVerified,
        String customerGroupKey,
        List<String> storeKeys,
        List<CustomerAddress> addresses) {
}
