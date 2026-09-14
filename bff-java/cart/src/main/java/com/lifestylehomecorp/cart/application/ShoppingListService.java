package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListAddLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListRemoveLineItemActionBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.lifestylehomecorp.cart.domain.CartLine;
import com.lifestylehomecorp.cart.domain.CartSummary;
import com.lifestylehomecorp.cart.domain.SavedItem;
import com.lifestylehomecorp.cart.domain.SavedList;
import com.lifestylehomecorp.platform.session.ShopperSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * The guest wishlist (pre-built for Session 4) and its two homework reuses. A commercetools
 * ShoppingList scoped by anonymousId backs it; the interesting parts — {@link #addAllToCart()} and
 * {@link #saveForLater(String)} — compose patterns already built in {@link CartService} rather than
 * introducing new SDK surface.
 */
@Service
public class ShoppingListService {

    private final ShoppingListRepository repo;
    private final CartService cartService;
    private final ShopperSession session;

    public ShoppingListService(ShoppingListRepository repo, CartService cartService, ShopperSession session) {
        this.repo = repo;
        this.cartService = cartService;
        this.session = session;
    }

    /** The guest's saved list (created on first touch). */
    public SavedList view() {
        return ShoppingListMapper.toDomain(getOrCreate());
    }

    /** Save an item (by SKU) for later. */
    public SavedList addItem(String sku, long quantity) {
        ShoppingList list = getOrCreate();
        return ShoppingListMapper.toDomain(repo.update(list.getId(), list.getVersion(),
                List.of(ShoppingListAddLineItemActionBuilder.of().sku(sku).quantity(quantity).build())));
    }

    /** Remove a saved item. */
    public SavedList removeItem(String lineItemId) {
        ShoppingList list = getOrCreate();
        return ShoppingListMapper.toDomain(repo.update(list.getId(), list.getVersion(),
                List.of(ShoppingListRemoveLineItemActionBuilder.of().lineItemId(lineItemId).build())));
    }

    /** Homework — MOVE every saved item into the cart (reusing {@link CartService#addLineItem}), then empty the list. */
    public CartSummary moveAllToCart() {
        SavedList list = view();
        CartSummary cart = cartService.summary();
        for (SavedItem item : list.items()) {
            if (item.sku() != null) {
                cart = cartService.addLineItem(item.sku(), item.quantity());
            }
        }
        clearList();
        return cart;
    }

    /** Move ONE saved item into the cart: add it, then remove it from the list. */
    public CartSummary moveToCart(String lineItemId) {
        SavedItem item = view().items().stream()
                .filter(i -> lineItemId.equals(i.lineItemId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No such saved item: " + lineItemId));
        if (item.sku() != null) {
            cartService.addLineItem(item.sku(), item.quantity());
        }
        removeItem(lineItemId);
        return cartService.summary();
    }

    /** Homework — move a cart line to the saved list: add it here, then remove it from the cart. */
    public SavedList saveForLater(String cartLineItemId) {
        CartSummary cart = cartService.summary();
        CartLine line = cart.lines().stream()
                .filter(l -> cartLineItemId.equals(l.lineItemId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No such cart line: " + cartLineItemId));
        if (line.sku() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This line has no SKU to save.");
        }
        addItem(line.sku(), line.quantity());
        cartService.removeLineItem(cartLineItemId);
        return view();
    }

    /** Remove every line from the list in one update (after a move-all-to-cart). */
    private void clearList() {
        ShoppingList list = getOrCreate();
        if (list.getLineItems() == null || list.getLineItems().isEmpty()) {
            return;
        }
        List<ShoppingListUpdateAction> removals = list.getLineItems().stream()
                .map(li -> (ShoppingListUpdateAction) ShoppingListRemoveLineItemActionBuilder.of()
                        .lineItemId(li.getId()).build())
                .toList();
        repo.update(list.getId(), list.getVersion(), removals);
    }

    /**
     * The shopper's list: the customer's once signed in (Session 5 — sign-in with the anonymousId handed
     * the guest list over, so "the wishlist follows the shopper" needs no merge call), else the guest's.
     */
    private ShoppingList getOrCreate() {
        if (session.isSignedIn()) {
            String customerId = session.requireCustomerId();
            ShoppingList list = repo.getByCustomerId(customerId);
            return list != null ? list : repo.createForCustomer(customerId);
        }
        String anonymousId = session.anonymousIdOrCreate();
        ShoppingList list = repo.getByAnonymousId(anonymousId);
        return list != null ? list : repo.create(anonymousId);
    }
}
