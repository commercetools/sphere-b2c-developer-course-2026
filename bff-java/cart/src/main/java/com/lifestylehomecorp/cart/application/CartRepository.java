package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartUpdateAction;

import java.util.List;

/**
 * The cart SDK gateway (implemented in infrastructure). Returns RAW SDK {@link Cart}; the
 * {@link CartService} maps to the domain. Participants implement the SDK call in each method (T1);
 * the service owns version + 409-retry around {@link #update}.
 */
public interface CartRepository {

    /** 4.1 — create the guest cart from a {@link CartContext}. */
    Cart create(CartContext ctx);

    /** Fetch the current cart (used by the service to read the latest version before an update). */
    Cart get(String cartId);

    /** 4.2 — apply a batch of update actions with the cart's current version. */
    Cart update(String cartId, Long version, List<CartUpdateAction> actions);

    /** 4.9 — apply a discount code (promo). The platform validates it against active Cart Discounts. */
    Cart addDiscountCode(String cartId, Long version, String code);

    /** 4.9 — remove a previously applied discount code by its id. */
    Cart removeDiscountCode(String cartId, Long version, String discountCodeId);
}
