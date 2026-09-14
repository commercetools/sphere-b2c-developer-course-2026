package com.lifestylehomecorp.cart.application;

import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartAddLineItemActionBuilder;
import com.commercetools.api.models.cart.CartChangeLineItemQuantityActionBuilder;
import com.commercetools.api.models.cart.CartRemoveLineItemActionBuilder;
import com.commercetools.api.models.cart.CartSetLineItemDistributionChannelActionBuilder;
import com.commercetools.api.models.cart.CartSetLineItemInventoryModeActionBuilder;
import com.commercetools.api.models.cart.CartSetLineItemSupplyChannelActionBuilder;
import com.commercetools.api.models.cart.CartSetShippingAddressActionBuilder;
import com.commercetools.api.models.cart.CartSetShippingMethodActionBuilder;
import com.commercetools.api.models.cart.CartUpdateAction;
import com.commercetools.api.models.cart.InventoryMode;
import com.commercetools.api.models.cart.LineItem;
import com.commercetools.api.models.common.BaseAddress;
import com.commercetools.api.models.common.BaseAddressBuilder;
import com.commercetools.api.models.recurring_order.PriceSelectionMode;
import com.lifestylehomecorp.cart.domain.CartSummary;
import com.lifestylehomecorp.cart.domain.ShippingOption;
import com.lifestylehomecorp.platform.session.ShopperSession;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import io.vrap.rmf.base.client.ApiHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.LongFunction;

/**
 * Cart orchestration: composes update actions and maps the SDK cart to the domain. It OWNS the
 * concurrency: {@link #withRetry} reads the cart's latest version, applies the change, and retries on
 * a 409 (stale version) — so the storefront never sees {@code version}. This is the "BFF owns version
 * + retry" best practice the course teaches in S4.
 */
@Service
public class CartService {

    private static final int MAX_RETRIES = 3;

    /** The seeded line-item Custom Type that links a bundle's parent and child lines (see the seeder). */
    private static final String BUNDLE_TYPE = "bundle-line";
    private static final String FIELD_BUNDLE_ID = "bundleId"; // on the parent line — the bundle identity
    private static final String FIELD_PARENT_ID = "parentId";  // on each child line — points at the parent

    private final CartRepository repo;
    private final ShippingRepository shippingRepo;
    private final BundleRepository bundleRepo;
    private final ShopperSession session;

    public CartService(CartRepository repo, ShippingRepository shippingRepo,
                       BundleRepository bundleRepo, ShopperSession session) {
        this.repo = repo;
        this.shippingRepo = shippingRepo;
        this.bundleRepo = bundleRepo;
        this.session = session;
    }

    /**
     * 4.1 — create the guest cart and remember it for the session. The cart carries the SESSION's
     * anonymousId (minted on first use, shared with the wishlist) — one guest identity across every
     * resource, so Session 5's sign-in hands the cart AND the shopping list over together. A caller-supplied
     * anonymousId is honoured only when the session has none yet.
     */
    public CartSummary createGuestCart(CartContext ctx) {
        String anonymousId = session.anonymousId() != null ? session.anonymousId()
                : ctx.anonymousId() != null ? ctx.anonymousId() : session.anonymousIdOrCreate();
        var cart = repo.create(new CartContext(ctx.currency(), ctx.country(), ctx.storeKey(), anonymousId));
        session.setActiveCart(cart.getId(), anonymousId);
        return CartMapper.toDomain(cart);
    }

    /** 4.10 — the cart summary. Never throws on an empty (or not-yet-created) cart. */
    public CartSummary summary() {
        // TODO (Task 4.10): return the active cart mapped to a domain summary (subtotal / savings /
        // shipping / tax / total via the trainer-provided CartMapper), and an EMPTY summary when no cart
        // exists yet — never throw on an empty cart. Decisions in the @TaskDescription.
        throw new TaskNotImplementedException("4.10");
    }

    /** 4.2 — add a line item (SKU × qty). */
    public CartSummary addLineItem(String sku, long quantity) {
        return mutate(List.of(CartAddLineItemActionBuilder.of().sku(sku).quantity(quantity).build()));
    }

    /** 4.2 — change a line's quantity (0 removes it). */
    public CartSummary changeQuantity(String lineItemId, long quantity) {
        return mutate(List.of(
                CartChangeLineItemQuantityActionBuilder.of().lineItemId(lineItemId).quantity(quantity).build()));
    }

    /**
     * 4.2 / 4.3 — remove a line. If the line is a bundle PARENT, its children are removed in the SAME
     * update (the bundle behaves as one unit); an ordinary line just removes itself.
     */
    public CartSummary removeLineItem(String lineItemId) {
        Cart cart = repo.get(session.requireCartId());
        List<CartUpdateAction> actions = new ArrayList<>();
        actions.add(CartRemoveLineItemActionBuilder.of().lineItemId(lineItemId).build());

        String bundleId = bundleIdOf(cart, lineItemId);
        if (bundleId != null) {
            for (LineItem li : cart.getLineItems()) {
                if (bundleId.equals(CartMapper.customString(li, FIELD_PARENT_ID))) {
                    actions.add(CartRemoveLineItemActionBuilder.of().lineItemId(li.getId()).build());
                }
            }
        }
        return mutate(actions);
    }

    /**
     * 4.3 — add a bundle as a group of line items in ONE update: a parent (the bundle product, priced
     * externally at 0 — it has no own price) carrying {@code bundleId}, plus one child per component
     * (real line item) carrying {@code parentId}. The seeded {@code bundle-saving} Cart Discount books
     * the saving on the children (predicate {@code custom.parentId is defined}) — the catalogue stays
     * truthful and finance can report the promotion.
     */
    public CartSummary addBundle(String bundleKey) {
        // TODO (Task 4.3): resolve the bundle's components (bundleRepo.resolve — trainer-provided), then
        // in ONE update add a parent line (the bundle product, externalPrice 0) carrying custom.bundleId,
        // plus one child per component carrying custom.parentId. A non-bundle key is a 4xx, not a partial
        // cart. The seeded bundle-saving Cart Discount books the saving. Decisions in the @TaskDescription.
        throw new TaskNotImplementedException("4.3");
    }

    /**
     * 4.4 — add a subscription line: same addLineItem, plus recurrenceInfo binding a RecurrencePolicy
     * and a required priceSelectionMode. The mode governs how future orders are priced (S6), not this
     * cart line: Fixed locks the subscribe-time price; Dynamic re-selects the price each cycle. We
     * default to Fixed (price stability); it is overridable.
     */
    public CartSummary addRecurringLineItem(String sku, String policyKey, String priceMode) {
        // TODO (Task 4.4): the same addLineItem as 4.2, PLUS recurrenceInfo binding a RecurrencePolicy
        // (by key, e.g. "monthly") AND a required priceSelectionMode (Fixed/Dynamic). Default the policy
        // to "monthly" and the mode to Fixed. Goal + hint in the @TaskDescription.
        throw new TaskNotImplementedException("4.4");
    }

    /** 4.5 — per-line fulfilment: distribution channel (price) and/or supply channel (source/pickup). */
    public CartSummary setLineItemChannel(String lineItemId, String distributionKey, String supplyKey) {
        List<CartUpdateAction> actions = new ArrayList<>();
        if (distributionKey != null && !distributionKey.isBlank()) {
            actions.add(CartSetLineItemDistributionChannelActionBuilder.of()
                    .lineItemId(lineItemId).distributionChannel(c -> c.key(distributionKey)).build());
        }
        if (supplyKey != null && !supplyKey.isBlank()) {
            actions.add(CartSetLineItemSupplyChannelActionBuilder.of()
                    .lineItemId(lineItemId).supplyChannel(c -> c.key(supplyKey)).build());
        }
        return actions.isEmpty() ? summary() : mutate(actions);
    }

    /** 4.6 — per-line inventory mode override (None / ReserveOnCart / ReserveOnOrder / TrackOnly). */
    public CartSummary setLineItemInventoryMode(String lineItemId, String mode) {
        // TODO (Task 4.6): apply setLineItemInventoryMode with the requested mode (map the string to the
        // InventoryMode enum — see the inventoryMode(...) helper below). Homework extends this with the
        // read-side stock gate. Goal + hint in the @TaskDescription.
        throw new TaskNotImplementedException("4.6");
    }

    /** 4.7 — set the shipping address (enables shipping matching + tax → taxedPrice). */
    public CartSummary setShippingAddress(AddressInput in) {
        BaseAddress address = BaseAddressBuilder.of()
                .country(in.country())
                .firstName(in.firstName())
                .lastName(in.lastName())
                .streetName(in.streetName())
                .streetNumber(in.streetNumber())
                .postalCode(in.postalCode())
                .city(in.city())
                .build();
        return mutate(List.of(CartSetShippingAddressActionBuilder.of().address(address).build()));
    }

    /** 4.8 — the shipping methods valid for the cart. */
    public List<ShippingOption> shippingOptions() {
        String cartId = session.requireCartId();
        return shippingRepo.matchingMethods(cartId).getResults().stream()
                .map(CartMapper::toShippingOption)
                .toList();
    }

    /** 4.8 — select a shipping method (adds shippingInfo to totalPrice). */
    public CartSummary setShippingMethod(String shippingMethodId) {
        return mutate(List.of(CartSetShippingMethodActionBuilder.of()
                .shippingMethod(sm -> sm.id(shippingMethodId)).build()));
    }

    /** 4.9 — apply a discount code (promo); a match lowers the total and adds a savings line. */
    public CartSummary applyDiscountCode(String code) {
        return withRetry(version -> repo.addDiscountCode(session.requireCartId(), version, code));
    }

    /** 4.9 — remove a previously applied discount code. */
    public CartSummary removeDiscountCode(String discountCodeId) {
        return withRetry(version -> repo.removeDiscountCode(session.requireCartId(), version, discountCodeId));
    }

    /** Apply a batch of update actions to the active cart, owning the version (see {@link #withRetry}). */
    CartSummary mutate(List<CartUpdateAction> actions) {
        return withRetry(version -> repo.update(session.requireCartId(), version, actions));
    }

    /**
     * Own the version: read the latest cart, apply {@code call} with its current version, and on a 409
     * (someone else moved the version) re-read and replay. Every write in this service funnels through
     * here, so the storefront never handles {@code version} or retries.
     */
    private CartSummary withRetry(LongFunction<Cart> call) {
        String cartId = session.requireCartId();
        ApiHttpException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            var current = repo.get(cartId);
            try {
                return CartMapper.toDomain(call.apply(current.getVersion()));
            } catch (ApiHttpException ex) {
                if (ex.getStatusCode() != 409) {
                    throw ex;
                }
                last = ex; // stale version — refetch + replay
            }
        }
        throw last;
    }

    /** The bundle identity ({@code custom.bundleId}) of a given line, or null if it is not a bundle parent. */
    private static String bundleIdOf(Cart cart, String lineItemId) {
        for (LineItem li : cart.getLineItems()) {
            if (li.getId().equals(lineItemId)) {
                return CartMapper.customString(li, FIELD_BUNDLE_ID);
            }
        }
        return null;
    }

    /** The active cart's currency — the scope an external bundle-parent price must be quoted in. */
    private String cartCurrency() {
        Cart cart = repo.get(session.requireCartId());
        return cart.getTotalPrice() != null ? cart.getTotalPrice().getCurrencyCode() : "EUR";
    }

    private static InventoryMode inventoryMode(String mode) {
        if (mode == null) {
            return InventoryMode.NONE;
        }
        return switch (mode) {
            case "ReserveOnCart" -> InventoryMode.RESERVE_ON_CART;
            case "ReserveOnOrder" -> InventoryMode.RESERVE_ON_ORDER;
            case "TrackOnly" -> InventoryMode.TRACK_ONLY;
            default -> InventoryMode.NONE;
        };
    }
}
