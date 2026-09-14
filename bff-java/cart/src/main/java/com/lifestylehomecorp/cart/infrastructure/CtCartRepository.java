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
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
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
        // TODO (Task 4.1): POST a CartDraft — currency + country + anonymousId + inventoryMode, and the
        // store when ctx.storeKey() is set — and return the raw SDK Cart. The service stores the id on
        // the session and maps it. Goal + hint in the @TaskDescription; see session-tasks-detailed.md.
        throw new TaskNotImplementedException("4.1");
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
        // TODO (Task 4.2): POST a CartUpdate carrying the cart's current version + the list of update
        // actions, returning the raw SDK Cart. The service composes the actions and owns version +
        // 409-retry. Goal + hint in the @TaskDescription; see session-tasks-detailed.md.
        throw new TaskNotImplementedException("4.2");
    }

    @Override
    public Cart addDiscountCode(String cartId, Long version, String code) {
        // TODO (Task 4.9): POST a CartUpdate with a CartAddDiscountCode action for `code`. The platform
        // validates it against active Cart Discounts. Goal + hint in the @TaskDescription.
        throw new TaskNotImplementedException("4.9");
    }

    @Override
    public Cart removeDiscountCode(String cartId, Long version, String discountCodeId) {
        // TODO (Task 4.9): POST a CartUpdate with a CartRemoveDiscountCode action referencing the
        // discount code by id.
        throw new TaskNotImplementedException("4.9");
    }
}
