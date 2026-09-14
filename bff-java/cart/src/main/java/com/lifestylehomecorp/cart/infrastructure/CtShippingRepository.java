package com.lifestylehomecorp.cart.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.shipping_method.ShippingMethodPagedQueryResponse;
import com.lifestylehomecorp.cart.application.ShippingRepository;
import org.springframework.stereotype.Repository;

/** Reads the shipping methods matching a cart (4.8) — a GET like S1–S3, scoped to the cart. */
@Repository
public class CtShippingRepository implements ShippingRepository {

    private final ProjectApiRoot apiRoot;

    public CtShippingRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public ShippingMethodPagedQueryResponse matchingMethods(String cartId) {
        return apiRoot.shippingMethods()
                .matchingCart()
                .get()
                .withCartId(cartId)
                .executeBlocking()
                .getBody();
    }
}
