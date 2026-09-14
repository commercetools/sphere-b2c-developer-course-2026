package com.lifestylehomecorp.cart.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartAddDiscountCodeActionBuilder;
import com.commercetools.api.models.cart.CartDraftBuilder;
import com.commercetools.api.models.cart.CartRemoveDiscountCodeActionBuilder;
import com.commercetools.api.models.cart.CartUpdateAction;
import com.commercetools.api.models.cart.CartUpdateBuilder;
import com.commercetools.api.models.cart.InventoryMode;
import com.lifestylehomecorp.cart.application.CartContext;
import com.lifestylehomecorp.cart.application.CartRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for the Cart write model — the ONE place SDK code is written for Session 4. Each method
 * is a single commercetools call returning the raw {@link Cart}; the {@link com.lifestylehomecorp.cart.application.CartService}
 * owns version + 409-retry and maps to the domain.
 */
@Repository
public class CtCartRepository implements CartRepository {

    /** Expand the applied discount codes so the summary can show the human code (e.g. "SAVE10"). */
    private static final String EXPAND_DISCOUNT_CODES = "discountCodes[*].discountCode";
    /** Expand each recurring line's policy so the summary can show its frequency (e.g. "Every month"). */
    private static final String EXPAND_RECURRENCE = "lineItems[*].recurrenceInfo.recurrencePolicy";

    private final ProjectApiRoot apiRoot;

    public CtCartRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public Cart create(CartContext ctx) {
        CartDraftBuilder draft = CartDraftBuilder.of()
                .currency(ctx.currency())
                .country(ctx.country())
                .anonymousId(ctx.anonymousId())
                .inventoryMode(InventoryMode.NONE);
        if (ctx.storeKey() != null && !ctx.storeKey().isBlank()) {
            draft.store(sr -> sr.key(ctx.storeKey()));
        }
        return apiRoot.carts().post(draft.build()).executeBlocking().getBody();
    }

    @Override
    public Cart get(String cartId) {
        return apiRoot.carts().withId(cartId).get()
                .withExpand(EXPAND_DISCOUNT_CODES)
                .addExpand(EXPAND_RECURRENCE)
                .executeBlocking().getBody();
    }

    @Override
    public Cart update(String cartId, Long version, List<CartUpdateAction> actions) {
        return apiRoot.carts().withId(cartId)
                .post(CartUpdateBuilder.of().version(version).actions(actions).build())
                .withExpand(EXPAND_DISCOUNT_CODES)
                .addExpand(EXPAND_RECURRENCE)
                .executeBlocking().getBody();
    }

    @Override
    public Cart addDiscountCode(String cartId, Long version, String code) {
        return apiRoot.carts().withId(cartId)
                .post(CartUpdateBuilder.of().version(version)
                        .actions(CartAddDiscountCodeActionBuilder.of().code(code).build())
                        .build())
                .withExpand(EXPAND_DISCOUNT_CODES)
                .addExpand(EXPAND_RECURRENCE)
                .executeBlocking().getBody();
    }

    @Override
    public Cart removeDiscountCode(String cartId, Long version, String discountCodeId) {
        return apiRoot.carts().withId(cartId)
                .post(CartUpdateBuilder.of().version(version)
                        .actions(CartRemoveDiscountCodeActionBuilder.of()
                                .discountCode(dc -> dc.id(discountCodeId)).build())
                        .build())
                .withExpand(EXPAND_DISCOUNT_CODES)
                .addExpand(EXPAND_RECURRENCE)
                .executeBlocking().getBody();
    }
}
