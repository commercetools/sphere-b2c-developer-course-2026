package com.lifestylehomecorp.customer.application;

import com.commercetools.api.models.cart.Cart;
import com.commercetools.api.models.cart.CartState;
import com.commercetools.api.models.cart.LineItem;
import com.commercetools.api.models.common.BaseAddressBuilder;
import com.commercetools.api.models.customer.AnonymousCartSignInMode;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerAddAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerChangeEmailActionBuilder;
import com.commercetools.api.models.customer.CustomerRemoveAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerSetCustomerGroupActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultBillingAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerSetDefaultShippingAddressActionBuilder;
import com.commercetools.api.models.customer.CustomerSetFirstNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSetLastNameActionBuilder;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.customer.CustomerToken;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.error.ErrorObject;
import com.commercetools.api.models.error.ErrorResponse;
import com.lifestylehomecorp.customer.domain.CustomerProfile;
import com.lifestylehomecorp.customer.domain.IssuedToken;
import com.lifestylehomecorp.customer.domain.MergeReport;
import com.lifestylehomecorp.customer.domain.SignInOutcome;
import com.lifestylehomecorp.platform.session.ShopperSession;
import io.vrap.rmf.base.client.ApiHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongFunction;

/**
 * Identity orchestration (Session 5). Composes update actions and maps the SDK customer to the
 * PII-safe domain profile. Like {@code CartService} it OWNS the version: {@link #withRetry} reads the
 * customer's latest version, applies the change, and retries on a 409. Identity always comes from the
 * {@link ShopperSession} — no method takes a customer id from the caller (the 5.9 boundary).
 */
@Service
public class CustomerService {

    private static final int MAX_RETRIES = 3;
    /** Reset / verification tokens are short-lived secrets — minutes, not the platform's day-scale default. */
    static final long TOKEN_TTL_MINUTES = 30;

    private final CustomerRepository repo;
    private final CartReadRepository carts;
    private final ShopperSession session;

    public CustomerService(CustomerRepository repo, CartReadRepository carts, ShopperSession session) {
        this.repo = repo;
        this.carts = carts;
        this.session = session;
    }

    // ---- 5.1 / 5.2 / 5.3 — become someone, bring your basket ----------------------------------

    /** 5.1 — sign up. The guest's carts / lists / orders (by anonymousId) become the new customer's. */
    public SignInOutcome signUp(String email, String password, String firstName, String lastName, String storeKey) {
        String guestCartId = guestCartIdOrNull();
        CustomerSignInResult result;
        try {
            result = repo.signUp(new SignUpInput(email, password, firstName, lastName, storeKey, guestAnonymousIdOrNull()));
        } catch (ApiHttpException ex) {
            throw friendlyDuplicate(ex);
        }
        // A brand-new customer has no other cart, so the mode is moot: the guest cart simply becomes theirs.
        return adopt(result, AnonymousCartSignInMode.USE_AS_NEW_ACTIVE_CUSTOMER_CART.getJsonName(), guestCartId, null);
    }

    /**
     * 5.2 — plain sign-in: authenticate, adopt the returned (recalculated) active cart, no merge decision.
     * The platform default (MergeWithExistingCustomerCart) applies; the report simply says what it finds.
     */
    public SignInOutcome signIn(String email, String password) {
        AnonymousCartSignInMode mode = AnonymousCartSignInMode.MERGE_WITH_EXISTING_CUSTOMER_CART;
        String guestCartId = guestCartIdOrNull();
        CustomerSignInResult result = repo.signIn(email, password, guestAnonymousIdOrNull(), mode);
        return adopt(result, mode.getJsonName(), guestCartId, null);
    }

    /**
     * 5.3 — sign in AND resolve the cart deliberately. The mode is the decision: MergeWithExistingCustomerCart
     * keeps both baskets (guest lines merged into the customer's most recently modified active cart),
     * UseAsNewActiveCustomerCart makes the guest cart the active one (the old cart is disassociated, not
     * deleted). A preflight checks eligibility (currency / store) and snapshots the customer's cart so the
     * {@link MergeReport} is exact; the session is re-pointed at the RETURNED cart — the guest id may now be
     * a read-only {@code Merged} cart. Never a half-merged cart: an ineligible merge is a 409 with a reason.
     */
    public SignInOutcome signInAndResolveCart(String email, String password, String mergeMode) {
        AnonymousCartSignInMode mode = modeOf(mergeMode);
        String guestCartId = guestCartIdOrNull();
        Cart before = preflight(email, guestCartId, mode);
        CustomerSignInResult result = repo.signIn(email, password, guestAnonymousIdOrNull(), mode);
        return adopt(result, mode.getJsonName(), guestCartId, before);
    }

    /**
     * Before signing in, look at the cart the merge would land in (the customer's most recently modified
     * Active cart) — for two reasons. (1) Eligibility: both carts must share the currency and the store;
     * the reference DECISION is to block with a reason rather than silently fall back to UseAsNew… and
     * lose items. (2) An exact merge report: only with the pre-merge snapshot can merged be told from
     * added. A server-side lookup by email is fine here — the BFF has the scope and nothing about it
     * reaches the browser (the credentials are still checked by the platform on the sign-in call).
     */
    private Cart preflight(String email, String guestCartId, AnonymousCartSignInMode mode) {
        if (guestCartId == null) {
            return null;
        }
        Customer existing = repo.findByEmail(email);
        if (existing == null) {
            return null; // unknown email → the sign-in call will answer InvalidCredentials
        }
        Cart before = carts.activeCartOfCustomer(existing.getId());
        Cart guest = carts.get(guestCartId);
        if (before != null && guest != null && mode == AnonymousCartSignInMode.MERGE_WITH_EXISTING_CUSTOMER_CART) {
            String reason = ineligibility(guest, before);
            if (reason != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Your basket cannot be merged into your account's basket: " + reason
                                + ". Sign in with mergeMode=UseAsNewActiveCustomerCart to keep the current basket instead.");
            }
        }
        return before;
    }

    /** The platform's eligibility rule: same currency, and the same store when either cart has one. */
    private static String ineligibility(Cart guest, Cart existing) {
        String gc = guest.getTotalPrice() == null ? null : guest.getTotalPrice().getCurrencyCode();
        String ec = existing.getTotalPrice() == null ? null : existing.getTotalPrice().getCurrencyCode();
        if (gc != null && ec != null && !gc.equals(ec)) {
            return "different currencies (" + gc + " vs " + ec + ")";
        }
        String gs = guest.getStore() == null ? null : guest.getStore().getKey();
        String es = existing.getStore() == null ? null : existing.getStore().getKey();
        if (!Objects.equals(gs, es)) {
            return "different stores (" + gs + " vs " + es + ")";
        }
        return null;
    }

    /**
     * An anonymousId can be used for sign-in / sign-up ONCE — the platform rejects a second use. A guest
     * has one to hand over; a shopper who is already signed in has nothing anonymous left (their guest
     * resources already moved at their own sign-in), so signing in as someone else carries nothing and
     * the credentials are checked first. On success {@link #adopt} replaces the session; on failure it
     * is left untouched.
     */
    private String guestAnonymousIdOrNull() {
        return session.isSignedIn() ? null : session.anonymousIdOrCreate();
    }

    /** The guest basket to report on — only a GUEST has one; a signed-in shopper's cart is not up for merging. */
    private String guestCartIdOrNull() {
        return session.isSignedIn() ? null : session.cartIdOrNull();
    }

    /** The last sign-in's merge report is not stored — rebuild it from the two carts on demand (5.3 read). */
    public MergeReport mergeReport(String anonymousCartId) {
        String activeId = session.cartIdOrNull();
        Cart active = activeId == null ? null : carts.get(activeId);
        return buildReport("(last sign-in)", active, anonymousCartId, null);
    }

    private SignInOutcome adopt(CustomerSignInResult result, String mode, String guestCartId, Cart before) {
        Customer c = result.getCustomer();
        session.signedIn(c.getId(), CustomerMapper.customerGroupId(c));
        Cart active = result.getCart();
        if (active != null) {
            session.setActiveCart(active.getId());
        }
        MergeReport report = buildReport(mode, active, guestCartId, before);
        int items = active == null || active.getLineItems() == null ? 0
                : active.getLineItems().stream().mapToInt(li -> li.getQuantity().intValue()).sum();
        // Re-read with customerGroup expanded so the profile carries the group KEY, not just its id.
        return new SignInOutcome(CustomerMapper.toProfile(repo.get(c.getId())),
                active == null ? null : active.getId(), items, report);
    }

    /**
     * The merge report: commercetools leaves the anonymous cart in place after a merge (cartState
     * Merged) — diffing it against the active cart shows which guest lines merged (property match:
     * product + variant + channels + custom + priceMode; higher quantity kept), which were added, and
     * which were left behind (a key conflict). If the guest cart IS the active cart (UseAsNew…, or the
     * customer had none) nothing was merged — it was adopted whole. With the pre-merge snapshot
     * ({@code before}, from {@link #preflight}) merged-vs-added is exact; without it, a heuristic.
     */
    private MergeReport buildReport(String mode, Cart active, String guestCartId, Cart before) {
        String activeId = active == null ? null : active.getId();
        if (guestCartId == null) {
            return MergeReport.none(mode, activeId);
        }
        if (activeId != null && activeId.equals(guestCartId)) {
            int lines = active.getLineItems() == null ? 0 : active.getLineItems().size();
            return new MergeReport(mode, activeId, guestCartId, stateOf(active), 0, lines, List.of());
        }
        Cart guest = carts.get(guestCartId);
        if (guest == null) {
            return MergeReport.none(mode, activeId);
        }
        int merged = 0;
        int added = 0;
        List<String> leftBehind = new ArrayList<>();
        List<LineItem> activeLines = active == null || active.getLineItems() == null ? List.of() : active.getLineItems();
        List<LineItem> beforeLines = before == null || before.getLineItems() == null ? null : before.getLineItems();
        boolean wasMerged = guest.getCartState() == CartState.MERGED;
        for (LineItem g : guest.getLineItems() == null ? List.<LineItem>of() : guest.getLineItems()) {
            LineItem match = activeLines.stream().filter(a -> sameLine(a, g)).findFirst().orElse(null);
            if (match == null) {
                leftBehind.add(skuOf(g)); // a key conflict — still on the Merged cart for inspection
            } else if (beforeLines != null) {
                // Exact: it merged if the customer already had a matching line before sign-in.
                if (beforeLines.stream().anyMatch(b -> sameLine(b, g))) {
                    merged++;
                } else {
                    added++;
                }
            } else if (wasMerged && match.getQuantity() > g.getQuantity()) {
                merged++; // heuristic (re-derived report): a higher quantity than the guest had ⇒ it met an existing line
            } else {
                added++;
            }
        }
        return new MergeReport(mode, activeId, guestCartId, stateOf(guest), merged, added, leftBehind);
    }

    // ---- 5.4 / 5.5 / 5.6 — be recognised ------------------------------------------------------

    /** 5.4 — my profile (PII-safe projection). */
    public CustomerProfile profile() {
        return CustomerMapper.toProfile(repo.get(session.requireCustomerId()));
    }

    /** 5.4 — edit name / email. changeEmail resets isEmailVerified (5.8). Null fields are left untouched. */
    public CustomerProfile updateProfile(String firstName, String lastName, String email) {
        List<CustomerUpdateAction> actions = new ArrayList<>();
        if (firstName != null) {
            actions.add(CustomerSetFirstNameActionBuilder.of().firstName(firstName).build());
        }
        if (lastName != null) {
            actions.add(CustomerSetLastNameActionBuilder.of().lastName(lastName).build());
        }
        if (email != null) {
            actions.add(CustomerChangeEmailActionBuilder.of().email(email).build());
        }
        return actions.isEmpty() ? profile() : mutate(actions);
    }

    /** 5.5 — add an address and (optionally) make it the default shipping / billing address in ONE update. */
    public CustomerProfile addAddress(AddressInput in, boolean defaultShipping, boolean defaultBilling) {
        String key = in.key() != null && !in.key().isBlank() ? in.key() : "addr-" + System.currentTimeMillis();
        List<CustomerUpdateAction> actions = new ArrayList<>();
        actions.add(CustomerAddAddressActionBuilder.of()
                .address(BaseAddressBuilder.of()
                        .key(key)
                        .country(in.country())
                        .firstName(in.firstName())
                        .lastName(in.lastName())
                        .streetName(in.streetName())
                        .streetNumber(in.streetNumber())
                        .postalCode(in.postalCode())
                        .city(in.city())
                        .build())
                .build());
        if (defaultShipping) {
            actions.add(CustomerSetDefaultShippingAddressActionBuilder.of().addressKey(key).build());
        }
        if (defaultBilling) {
            actions.add(CustomerSetDefaultBillingAddressActionBuilder.of().addressKey(key).build());
        }
        return mutate(actions);
    }

    /** 5.5 — make an existing address the default shipping address. */
    public CustomerProfile setDefaultShippingAddress(String addressId) {
        return mutate(List.of(CustomerSetDefaultShippingAddressActionBuilder.of().addressId(addressId).build()));
    }

    /** 5.5 — remove an address from the book. */
    public CustomerProfile removeAddress(String addressId) {
        return mutate(List.of(CustomerRemoveAddressActionBuilder.of().addressId(addressId).build()));
    }

    /**
     * 5.6 (assign, pre-built) — put the customer in a group (by key). A pricing lever: never exposed to
     * the storefront in production — here a trainer/admin call. Updates the session so the catalogue
     * re-prices on the very next read.
     */
    public CustomerProfile setCustomerGroup(String groupKey) {
        CustomerProfile p = mutate(List.of(CustomerSetCustomerGroupActionBuilder.of()
                .customerGroup(cg -> cg.key(groupKey)).build()));
        session.setCustomerGroupId(CustomerMapper.customerGroupId(repo.get(session.requireCustomerId())));
        return p;
    }

    // ---- 5.7 / 5.8 — stay safe -----------------------------------------------------------------

    /** 5.7 — change password (signed in): current password + version required; other tokens are invalidated. */
    public CustomerProfile changePassword(String currentPassword, String newPassword) {
        return withRetry(version -> repo.changePassword(session.requireCustomerId(), version, currentPassword, newPassword));
    }

    /**
     * 5.7 — "Forgot password": issue a reset token. Answers the same whether or not the email exists
     * (no user enumeration) — a 404 from the platform becomes a token-less OK.
     */
    public IssuedToken requestPasswordReset(String email) {
        try {
            CustomerToken t = repo.createPasswordResetToken(email, TOKEN_TTL_MINUTES);
            return new IssuedToken(t.getValue(), String.valueOf(t.getExpiresAt()), IssuedToken.DEMO_NOTE);
        } catch (ApiHttpException ex) {
            if (ex.getStatusCode() == 404) {
                return new IssuedToken(null, null, "If an account exists for this email, a reset link has been sent.");
            }
            throw ex;
        }
    }

    /** 5.7 — reset with the token. No session needed: the token IS the proof. */
    public CustomerProfile resetPassword(String tokenValue, String newPassword) {
        return CustomerMapper.toProfile(repo.resetPassword(tokenValue, newPassword));
    }

    /** 5.8 (pre-built) — issue an email-verification token for the signed-in customer. */
    public IssuedToken requestEmailVerification() {
        Customer c = repo.get(session.requireCustomerId());
        CustomerToken t = repo.createEmailToken(c.getId(), c.getVersion(), TOKEN_TTL_MINUTES);
        return new IssuedToken(t.getValue(), String.valueOf(t.getExpiresAt()), IssuedToken.DEMO_NOTE);
    }

    /** 5.8 (pre-built) — confirm the email → isEmailVerified = true (a business flag, not an auth gate). */
    public CustomerProfile confirmEmail(String tokenValue) {
        return CustomerMapper.toProfile(repo.confirmEmail(tokenValue));
    }

    // ---- 5.10 — be forgotten -------------------------------------------------------------------

    /**
     * 5.10 (homework) — delete the account. Orders keep their customer snapshot (nothing cascades);
     * the delete-vs-anonymise decision is the participant's. Afterwards the session is a fresh guest.
     */
    public void deleteAccount() {
        String id = session.requireCustomerId();
        Customer c = repo.get(id);
        repo.delete(id, c.getVersion());
        session.signOut();
    }

    // ---- plumbing ------------------------------------------------------------------------------

    private CustomerProfile mutate(List<CustomerUpdateAction> actions) {
        return withRetry(version -> repo.update(session.requireCustomerId(), version, actions));
    }

    /** Own the version: read the latest customer, apply with its version, re-read + replay on a 409. */
    private CustomerProfile withRetry(LongFunction<Customer> call) {
        String id = session.requireCustomerId();
        ApiHttpException last = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            Customer current = repo.get(id);
            try {
                call.apply(current.getVersion());
                return CustomerMapper.toProfile(repo.get(id)); // re-read: customerGroup expanded
            } catch (ApiHttpException ex) {
                if (ex.getStatusCode() != 409) {
                    throw ex;
                }
                last = ex; // stale version — refetch + replay
            }
        }
        throw last;
    }

    private static AnonymousCartSignInMode modeOf(String mergeMode) {
        if (mergeMode == null || mergeMode.isBlank()) {
            return AnonymousCartSignInMode.MERGE_WITH_EXISTING_CUSTOMER_CART;
        }
        return AnonymousCartSignInMode.findEnum(mergeMode);
    }

    /** A duplicate email is a client conflict with a friendly hint, not the platform's raw 400. */
    private static RuntimeException friendlyDuplicate(ApiHttpException ex) {
        try {
            ErrorResponse body = ex.getBodyAs(ErrorResponse.class);
            if (body != null && body.getErrors() != null) {
                for (ErrorObject e : body.getErrors()) {
                    if ("DuplicateField".equals(e.getCode())) {
                        return new ResponseStatusException(HttpStatus.CONFLICT,
                                "An account with this email already exists — sign in instead.");
                    }
                }
            }
        } catch (Exception ignored) {
            // not a commercetools error body — fall through to the original exception
        }
        return ex;
    }

    /** The commercetools merge match: product + variant + channels + custom + priceMode (keys are ignored). */
    private static boolean sameLine(LineItem a, LineItem b) {
        return Objects.equals(a.getProductId(), b.getProductId())
                && Objects.equals(a.getVariant() == null ? null : a.getVariant().getId(),
                        b.getVariant() == null ? null : b.getVariant().getId())
                && Objects.equals(idOf(a.getSupplyChannel()), idOf(b.getSupplyChannel()))
                && Objects.equals(idOf(a.getDistributionChannel()), idOf(b.getDistributionChannel()))
                && Objects.equals(a.getPriceMode(), b.getPriceMode());
    }

    private static String idOf(com.commercetools.api.models.channel.ChannelReference ref) {
        return ref == null ? null : ref.getId();
    }

    private static String skuOf(LineItem li) {
        return li.getVariant() != null && li.getVariant().getSku() != null ? li.getVariant().getSku() : li.getProductId();
    }

    private static String stateOf(Cart cart) {
        return cart.getCartState() == null ? null : cart.getCartState().getJsonName();
    }
}
