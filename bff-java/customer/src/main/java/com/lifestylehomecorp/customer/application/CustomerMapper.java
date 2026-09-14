package com.lifestylehomecorp.customer.application;

import com.commercetools.api.models.common.Address;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.store.StoreKeyReference;
import com.lifestylehomecorp.customer.domain.CustomerAddress;
import com.lifestylehomecorp.customer.domain.CustomerProfile;

import java.util.List;
import java.util.Objects;

/**
 * SDK {@link Customer} → PII-safe {@link CustomerProfile} (5.10). The ONE place the projection is
 * decided: whatever is not mapped here never reaches a controller, a log line, or the browser.
 */
public final class CustomerMapper {

    private CustomerMapper() {
    }

    public static CustomerProfile toProfile(Customer c) {
        String defaultShipping = c.getDefaultShippingAddressId();
        String defaultBilling = c.getDefaultBillingAddressId();
        List<CustomerAddress> addresses = c.getAddresses() == null ? List.of() : c.getAddresses().stream()
                .map(a -> toAddress(a, defaultShipping, defaultBilling))
                .toList();
        List<String> stores = c.getStores() == null ? List.of() : c.getStores().stream()
                .map(StoreKeyReference::getKey).filter(Objects::nonNull).toList();
        return new CustomerProfile(
                c.getId(),
                c.getEmail(),
                c.getFirstName(),
                c.getLastName(),
                Boolean.TRUE.equals(c.getIsEmailVerified()),
                customerGroupKey(c),
                stores,
                addresses);
    }

    /** The group KEY when the reference is expanded (the repository's get expands it), else the id, else null. */
    public static String customerGroupKey(Customer c) {
        if (c.getCustomerGroup() == null) {
            return null;
        }
        if (c.getCustomerGroup().getObj() != null && c.getCustomerGroup().getObj().getKey() != null) {
            return c.getCustomerGroup().getObj().getKey();
        }
        return c.getCustomerGroup().getId();
    }

    /** The group ID (price selection needs the id) or null for an ungrouped customer. */
    public static String customerGroupId(Customer c) {
        return c.getCustomerGroup() == null ? null : c.getCustomerGroup().getId();
    }

    private static CustomerAddress toAddress(Address a, String defaultShipping, String defaultBilling) {
        return new CustomerAddress(
                a.getId(),
                a.getKey(),
                a.getCountry(),
                a.getFirstName(),
                a.getLastName(),
                a.getStreetName(),
                a.getStreetNumber(),
                a.getPostalCode(),
                a.getCity(),
                a.getId() != null && a.getId().equals(defaultShipping),
                a.getId() != null && a.getId().equals(defaultBilling));
    }
}
