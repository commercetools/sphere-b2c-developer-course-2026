package com.lifestylehomecorp.customer.application;

import com.commercetools.api.models.cart.Cart;

/**
 * A read-only cart gateway owned by the customer module (implemented in infrastructure) so the merge
 * report (5.3) can inspect the anonymous cart after sign-in WITHOUT depending on the {@code cart}
 * module — domain modules depend on {@code platform} only. Trainer-provided plumbing.
 */
public interface CartReadRepository {

    /** The cart by id, or null when it no longer exists. */
    Cart get(String cartId);

    /** The customer's most recently modified Active cart — the one a merge lands in — or null. */
    Cart activeCartOfCustomer(String customerId);
}
