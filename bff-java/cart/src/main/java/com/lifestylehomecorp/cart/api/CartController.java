package com.lifestylehomecorp.cart.api;

import com.lifestylehomecorp.cart.api.dto.AddBundleRequest;
import com.lifestylehomecorp.cart.api.dto.AddLineItemRequest;
import com.lifestylehomecorp.cart.api.dto.CartView;
import com.lifestylehomecorp.cart.api.dto.ChangeQuantityRequest;
import com.lifestylehomecorp.cart.api.dto.DiscountCodeRequest;
import com.lifestylehomecorp.cart.api.dto.SetChannelRequest;
import com.lifestylehomecorp.cart.api.dto.SetInventoryModeRequest;
import com.lifestylehomecorp.cart.api.dto.SetShippingMethodRequest;
import com.lifestylehomecorp.cart.api.dto.ShippingAddressRequest;
import com.lifestylehomecorp.cart.api.dto.ShippingOptionView;
import com.lifestylehomecorp.cart.application.AddressInput;
import com.lifestylehomecorp.cart.application.CartContext;
import com.lifestylehomecorp.cart.application.CartService;
import com.lifestylehomecorp.platform.annotations.TaskDescription;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cart write model HTTP contract (Session 4 — "Fill the Basket"). The course's first write surface:
 * every mutation is an update action on a stateful cart carrying its version; the BFF owns version +
 * 409-retry (see {@link CartService}), so the storefront just states intent. Cart identity is
 * trainer-provided plumbing (see {@code com.lifestylehomecorp.platform.session.ShopperSession}, shared
 * with Session 5's identity — the same session carries the signed-in customer).
 */
@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 1,
            title = "Create the guest cart", tier = "T1", capability = "cart.create",
            description = "Implement CartRepository.create(...) in the cart module's infrastructure layer — "
                    + "POST a CartDraft (currency + country + store + anonymousId + inventoryMode) and return "
                    + "the raw SDK Cart; the service stores the cart id on the session and maps it to a domain "
                    + "cart. This is the course's first WRITE: POST /api/cart makes an empty guest cart the "
                    + "storefront can add to.",
            hint = "Docs: Carts — create a Cart with a CartDraft "
                    + "(docs.commercetools.com/api/projects/carts#create-a-cart).")
    @PostMapping("/api/cart")
    public CartView create(@RequestParam(required = false) String store,
                           @RequestParam(required = false, defaultValue = "EUR") String currency,
                           @RequestParam(required = false, defaultValue = "DE") String country) {
        return CartViewMapper.toView(cartService.createGuestCart(
                new CartContext(currency, country, store, null))); // anonymousId = the session's (one guest identity)
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 2,
            title = "Add & manage line items", tier = "T1", capability = "cart.lineItems",
            description = "Implement CartRepository.update(cartId, version, actions) in the cart module's "
                    + "infrastructure layer — POST a CartUpdate carrying the cart's current version + a list "
                    + "of update actions, returning the raw SDK Cart. The service composes addLineItem / "
                    + "changeLineItemQuantity / removeLineItem and OWNS the version: it reads the latest cart, "
                    + "applies the update, and retries on a 409 (stale version) — so the storefront just says "
                    + "'add SKU × qty'. POST/PATCH/DELETE /api/cart/line-items drive the cart drawer.",
            hint = "Docs: Carts — update actions & optimistic concurrency (version) "
                    + "(docs.commercetools.com/api/projects/carts#update-actions).")
    @PostMapping("/api/cart/line-items")
    public CartView addLineItem(@RequestBody AddLineItemRequest req) {
        long qty = req.quantity() == null ? 1L : req.quantity();
        return CartViewMapper.toView(cartService.addLineItem(req.sku(), qty));
    }

    @PatchMapping("/api/cart/line-items/{lineItemId}")
    public CartView changeQuantity(@PathVariable String lineItemId, @RequestBody ChangeQuantityRequest req) {
        return CartViewMapper.toView(cartService.changeQuantity(lineItemId, req.quantity()));
    }

    @DeleteMapping("/api/cart/line-items/{lineItemId}")
    public CartView removeLineItem(@PathVariable String lineItemId) {
        return CartViewMapper.toView(cartService.removeLineItem(lineItemId));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 3,
            title = "Add a bundle to the cart", tier = "T2", capability = "cart.bundles",
            description = "Turn the catalog bundle into a coherent cart line group in ONE update: a parent "
                    + "line (the bundle product, priced externally at 0 — it has no own price) carrying the "
                    + "bundle-line custom type's bundleId as the bundle identity, plus one child line per "
                    + "component (a real line item — real inventory / tax / fulfilment) carrying parentId "
                    + "pointing back at it. The seeded bundle-saving Cart Discount books the saving on the "
                    + "children (predicate custom.parentId is defined). Resolve the components with the cart's "
                    + "OWN bundle read (no dependency on the catalog module). POST /api/cart/bundles adds the "
                    + "group; removing the parent cascades to its children in one call.",
            hint = "Docs: Carts addLineItem with custom fields; Cart Discounts (lineItems target) "
                    + "(docs.commercetools.com/api/projects/carts#add-lineitem).",
            decisions = {
                    "The cart shape — parent + children linked by a custom field vs one opaque line vs custom "
                            + "line items (children as real line items keep inventory / tax / fulfilment correct).",
                    "Where the saving lives — a Cart Discount on the children vs a hard-coded bundle price "
                            + "(keep the catalogue truthful; the promotion stays reportable).",
                    "The discount predicate — how the Cart Discount recognises the bundle "
                            + "(custom.parentId is defined) without firing on loose components."
            })
    @PostMapping("/api/cart/bundles")
    public CartView addBundle(@RequestBody AddBundleRequest req) {
        return CartViewMapper.toView(cartService.addBundle(req.bundleKey()));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 4,
            title = "Recurring / subscription line item", tier = "T1", capability = "cart.recurring",
            description = "Add a subscription line — the same addLineItem, plus recurrenceInfo bound to a "
                    + "seeded RecurrencePolicy (e.g. \"monthly\") and a required priceSelectionMode so the "
                    + "line is billed on a schedule at its recurrence-scoped (subscription) price. Implement "
                    + "the recurring addLineItem in CartService.addRecurringLineItem(...). POST "
                    + "/api/cart/line-items?recurring=true adds a \"Subscribe & save — every month\" line. "
                    + "priceMode picks Fixed (lock the subscribe-time price) vs Dynamic (re-price each cycle) "
                    + "— it governs future orders (Session 6), not this cart line; here the payoff is the "
                    + "subscription line in the cart.",
            hint = "Docs: Recurring Orders — RecurrencePolicy + LineItem recurrenceInfo + PriceSelectionMode "
                    + "(docs.commercetools.com/api/projects/recurring-orders).")
    @PostMapping(value = "/api/cart/line-items", params = "recurring")
    public CartView addRecurringLineItem(@RequestBody AddLineItemRequest req,
                                         @RequestParam(name = "recurrencePolicy", required = false) String recurrencePolicy,
                                         @RequestParam(name = "priceMode", required = false) String priceMode) {
        return CartViewMapper.toView(cartService.addRecurringLineItem(req.sku(), recurrencePolicy, priceMode));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 5,
            title = "Fulfilment channel: pickup vs delivery", tier = "T1", capability = "cart.channel",
            description = "Set the per-line fulfilment channel: setLineItemDistributionChannel drives the "
                    + "channel PRICE (the S2/S3 channel price, now shopper-chosen), setLineItemSupplyChannel "
                    + "sets the inventory SOURCE / pickup location — the two channel roles (BOPIS). "
                    + "PUT /api/cart/line-items/{id}/channel picks deliver-by-shipping vs pick-up-in-store.",
            hint = "Docs: Carts — setLineItemDistributionChannel / setLineItemSupplyChannel "
                    + "(docs.commercetools.com/api/projects/carts#set-lineitem-distributionchannel).")
    @PutMapping("/api/cart/line-items/{lineItemId}/channel")
    public CartView setChannel(@PathVariable String lineItemId, @RequestBody SetChannelRequest req) {
        return CartViewMapper.toView(
                cartService.setLineItemChannel(lineItemId, req.distributionChannelKey(), req.supplyChannelKey()));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 6,
            title = "Inventory modes + stock gate", tier = "T1", capability = "cart.stockGate",
            description = "Set a per-line inventory mode via setLineItemInventoryMode (overrides the cart "
                    + "default): None (no checks) / ReserveOnCart (reserve on add) / ReserveOnOrder (reserve "
                    + "at order) / TrackOnly (decrement on order). PUT "
                    + "/api/cart/line-items/{id}/inventory-mode. (Homework — the read-side STOCK GATE: an "
                    + "availability pre-check at add-time, with a block/cap/backorder policy you own.)",
            hint = "Docs: Carts — inventoryMode + setLineItemInventoryMode; Inventory availability "
                    + "(docs.commercetools.com/api/projects/carts#inventorymode).")
    @PutMapping("/api/cart/line-items/{lineItemId}/inventory-mode")
    public CartView setInventoryMode(@PathVariable String lineItemId, @RequestBody SetInventoryModeRequest req) {
        return CartViewMapper.toView(cartService.setLineItemInventoryMode(lineItemId, req.inventoryMode()));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 7,
            title = "Set the shipping address", tier = "T1", capability = "cart.address",
            description = "Set the delivery address via setShippingAddress (country + fields). The address is "
                    + "the input to shipping-method matching (Zones) and tax — once set, the cart gains a "
                    + "taxedPrice. PUT /api/cart/shipping-address drives the address form.",
            hint = "Docs: Carts — setShippingAddress (docs.commercetools.com/api/projects/carts#set-shipping-address).")
    @PutMapping("/api/cart/shipping-address")
    public CartView setShippingAddress(@RequestBody ShippingAddressRequest req) {
        return CartViewMapper.toView(cartService.setShippingAddress(new AddressInput(
                req.country(), req.firstName(), req.lastName(),
                req.streetName(), req.streetNumber(), req.postalCode(), req.city())));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 8,
            title = "Shipping method: match & select", tier = "T1", capability = "cart.shipping",
            description = "List the shipping methods valid for the cart via shippingMethods().matchingCart() "
                    + "(a GET like S1–S3 — Zones + predicate + value/score tiers), then PUT "
                    + "/api/cart/shipping-method with the chosen id (setShippingMethod adds shippingInfo to "
                    + "totalPrice). GET /api/cart/shipping-methods returns the selectable options.",
            hint = "Docs: Shipping methods — matching-cart + Carts setShippingMethod "
                    + "(docs.commercetools.com/api/projects/shippingMethods#get-shippingmethods-for-a-cart).")
    @GetMapping("/api/cart/shipping-methods")
    public List<ShippingOptionView> shippingMethods() {
        return cartService.shippingOptions().stream().map(ShippingOptionView::from).toList();
    }

    @PutMapping("/api/cart/shipping-method")
    public CartView setShippingMethod(@RequestBody SetShippingMethodRequest req) {
        return CartViewMapper.toView(cartService.setShippingMethod(req.shippingMethodId()));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 9,
            title = "Apply a discount code (promo)", tier = "T1", capability = "cart.promo",
            description = "Implement CartRepository.addDiscountCode(cartId, version, code) (+ remove) — a "
                    + "cart update carrying addDiscountCode. The platform validates the code against active "
                    + "Cart Discounts and, on a match, books the saving (a lower total + a savings line). The "
                    + "service owns version + 409-retry as ever. POST /api/cart/discount-codes applies a code; "
                    + "DELETE removes it. Try SAVE10 (10% off carts over €100, or free shipping).",
            hint = "Docs: Carts — addDiscountCode / removeDiscountCode; Discount Codes "
                    + "(docs.commercetools.com/api/projects/carts#add-discountcode).")
    @PostMapping("/api/cart/discount-codes")
    public CartView applyDiscountCode(@RequestBody DiscountCodeRequest req) {
        return CartViewMapper.toView(cartService.applyDiscountCode(req.code()));
    }

    @DeleteMapping("/api/cart/discount-codes/{discountCodeId}")
    public CartView removeDiscountCode(@PathVariable String discountCodeId) {
        return CartViewMapper.toView(cartService.removeDiscountCode(discountCodeId));
    }

    @TaskDescription(
            module = "cart", session = "Session 4", taskNumber = 10,
            title = "Cart summary + savings", tier = "T2", capability = "cart.summary",
            description = "Implement CartService.summary() in the cart module's application layer — compose "
                    + "the cart's money fields (line items with price / discounted price, totalPrice, "
                    + "taxedPrice, discountOnTotalPrice, shippingInfo) into a domain summary for the "
                    + "storefront's order summary: subtotal, savings, shipping, tax, total. GET /api/cart "
                    + "returns it; never throw on an empty cart.",
            hint = "Docs: Carts — totalPrice, taxedPrice, discountOnTotalPrice, shippingInfo "
                    + "(docs.commercetools.com/api/projects/carts#cart).",
            decisions = {
                    "How to present savings — cart-level vs per-line; original vs discounted totals.",
                    "Tax included vs excluded — taxedPrice / taxMode (taxedPrice appears once a shipping "
                            + "address is set, task 4.7).",
                    "The empty-cart state — return an empty summary, never throw."
            })
    @GetMapping("/api/cart")
    public CartView summary() {
        return CartViewMapper.toView(cartService.summary());
    }
}
