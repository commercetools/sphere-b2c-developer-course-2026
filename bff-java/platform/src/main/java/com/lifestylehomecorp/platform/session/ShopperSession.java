package com.lifestylehomecorp.platform.session;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Trainer-provided identity plumbing shared by every domain module: WHO is shopping and WHAT is theirs.
 *
 * <ul>
 *   <li><b>anonymousId</b> — the guest identity (S4); carts / shopping lists / orders carry it, and
 *       sign-up / sign-in with it hands those resources to the customer (S5).</li>
 *   <li><b>cartId</b> — the active cart (S4). The version is NOT held here — services fetch the latest
 *       before every update (fetch-before-update + 409 retry).</li>
 *   <li><b>customerId / customerGroupId</b> — set at sign-in (S5). {@code catalog} reads the group into
 *       price selection (5.6); {@code customer} resolves "me" from here, never from a request param (5.9).</li>
 * </ul>
 *
 * In-memory + single shopper — enough for the course's single-user BFF; a real storefront keys this per
 * browser session (an httpOnly cookie → server-side session), see the commercetools-storefront skill.
 */
@Component
public class ShopperSession {

    private final AtomicReference<String> anonymousId = new AtomicReference<>();
    private final AtomicReference<String> cartId = new AtomicReference<>();
    private final AtomicReference<String> customerId = new AtomicReference<>();
    private final AtomicReference<String> customerGroupId = new AtomicReference<>();

    // ---- cart (S4) ----------------------------------------------------------------------------

    /** Remember the active cart (and the anonymous id it was created with). */
    public void setActiveCart(String id, String anonId) {
        cartId.set(id);
        if (anonId != null) {
            anonymousId.set(anonId);
        }
    }

    /** Re-point the session at another cart — after sign-in the customer's (merged) cart becomes active. */
    public void setActiveCart(String id) {
        cartId.set(id);
    }

    public String cartIdOrNull() {
        return cartId.get();
    }

    public String requireCartId() {
        String id = cartId.get();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No active cart — create one first (POST /api/cart).");
        }
        return id;
    }

    public String anonymousId() {
        return anonymousId.get();
    }

    /**
     * The guest's anonymousId, minting + remembering one if none exists yet — so a wishlist can be
     * scoped to the guest even before the first cart is created (the shopping list, S4).
     */
    public String anonymousIdOrCreate() {
        return anonymousId.updateAndGet(a -> a != null ? a : UUID.randomUUID().toString());
    }

    // ---- identity (S5) -------------------------------------------------------------------------

    public boolean isSignedIn() {
        return customerId.get() != null;
    }

    public String customerIdOrNull() {
        return customerId.get();
    }

    /** The signed-in customer — a 401 when there is none (the "me" endpoints need a shopper). */
    public String requireCustomerId() {
        String id = customerId.get();
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Not signed in — POST /api/customers/login (or /signup) first.");
        }
        return id;
    }

    /** The customer group ID (price-selection scope) — null for a guest; a guest has NO group. */
    public String customerGroupIdOrNull() {
        return customerGroupId.get();
    }

    /** Record a successful sign-up / sign-in. The group may be null (most customers have none). */
    public void signedIn(String id, String groupId) {
        customerId.set(id);
        customerGroupId.set(groupId);
    }

    public void setCustomerGroupId(String groupId) {
        customerGroupId.set(groupId);
    }

    /**
     * Sign out: forget the customer AND their cart, and mint a fresh anonymousId — the old one belonged
     * to a signed-in shopper and must not leak into the next guest session.
     */
    public void signOut() {
        customerId.set(null);
        customerGroupId.set(null);
        cartId.set(null);
        anonymousId.set(UUID.randomUUID().toString());
    }
}
