package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;

import java.util.List;

/**
 * SDK gateway for the guest's shopping list (wishlist). Returns raw SDK {@link ShoppingList}; the
 * {@link ShoppingListService} maps to the domain. Trainer-provided plumbing for Session 4 — the
 * shopping-list feature is pre-built; the interesting reuse (list → cart, save-for-later) lives in the
 * service.
 */
public interface ShoppingListRepository {

    /** The guest's list, scoped by anonymousId (line items' variants expanded), or null if none yet. */
    ShoppingList getByAnonymousId(String anonymousId);

    /** Create an empty "Saved items" list for the guest (anonymousId-scoped). */
    ShoppingList create(String anonymousId);

    /**
     * The signed-in customer's list, or null if none yet (Session 5). Sign-in with an anonymousId hands
     * the guest's list to the customer — and clears its anonymousId — so from then on it is found here.
     */
    ShoppingList getByCustomerId(String customerId);

    /** Create an empty "Saved items" list owned by the customer (Session 5). */
    ShoppingList createForCustomer(String customerId);

    /** Apply update actions with the list's current version (line items' variants expanded on return). */
    ShoppingList update(String listId, Long version, List<ShoppingListUpdateAction> actions);
}
