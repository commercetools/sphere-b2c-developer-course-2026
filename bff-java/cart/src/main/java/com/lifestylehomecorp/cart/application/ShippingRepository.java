package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.shipping_method.ShippingMethodPagedQueryResponse;

/** SDK gateway for shipping-method matching (4.8). Returns raw SDK types; the service maps them. */
public interface ShippingRepository {

    /** The shipping methods valid for this cart's address / value (Zones + predicates). */
    ShippingMethodPagedQueryResponse matchingMethods(String cartId);
}
