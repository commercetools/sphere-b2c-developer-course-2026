package com.lifestylehomecorp.cart.api;

import com.lifestylehomecorp.cart.api.dto.AddToListRequest;
import com.lifestylehomecorp.cart.api.dto.CartView;
import com.lifestylehomecorp.cart.api.dto.SaveForLaterRequest;
import com.lifestylehomecorp.cart.api.dto.SavedListView;
import com.lifestylehomecorp.cart.application.ShoppingListService;
import com.lifestylehomecorp.platform.annotations.TaskDescription;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guest wishlist / save-for-later (Session 4, pre-built). A commercetools ShoppingList scoped by
 * anonymousId; the two "move" endpoints ({@code /to-cart}, {@code /save-for-later}) are the homework
 * payoff — they reuse the cart's own line-item operations rather than adding new SDK surface.
 */
@RestController
public class ShoppingListController {

    private final ShoppingListService service;

    public ShoppingListController(ShoppingListService service) {
        this.service = service;
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 11,
            title = "Shopping list (wishlist / save-for-later)", tier = "pre-built",
            capability = "cart.shoppingList",
            description = "Trainer-provided: a guest wishlist, backed by a commercetools ShoppingList scoped "
                    + "by anonymousId. GET /api/shopping-list returns the saved items; POST adds one by SKU. "
                    + "The homework builds on it without new SDK surface: /to-cart (all) and "
                    + "/items/{id}/to-cart (one) MOVE saved items into the cart (add, then remove from the "
                    + "list), and /save-for-later moves a cart line the other way (add here, remove there).",
            hint = "Docs: Shopping Lists — create / addLineItem, scope by anonymousId "
                    + "(docs.commercetools.com/api/projects/shoppingLists).")
    @GetMapping("/api/shopping-list")
    public SavedListView view() {
        return SavedListView.from(service.view());
    }

    @PostMapping("/api/shopping-list")
    public SavedListView add(@RequestBody AddToListRequest req) {
        long quantity = req.quantity() == null ? 1L : req.quantity();
        return SavedListView.from(service.addItem(req.sku(), quantity));
    }

    @DeleteMapping("/api/shopping-list/items/{lineItemId}")
    public SavedListView remove(@PathVariable String lineItemId) {
        return SavedListView.from(service.removeItem(lineItemId));
    }

    @PostMapping("/api/shopping-list/to-cart")
    public CartView moveAllToCart() {
        return CartViewMapper.toView(service.moveAllToCart());
    }

    @PostMapping("/api/shopping-list/items/{lineItemId}/to-cart")
    public CartView moveToCart(@PathVariable String lineItemId) {
        return CartViewMapper.toView(service.moveToCart(lineItemId));
    }

    @PostMapping("/api/shopping-list/save-for-later")
    public SavedListView saveForLater(@RequestBody SaveForLaterRequest req) {
        return SavedListView.from(service.saveForLater(req.lineItemId()));
    }
}
