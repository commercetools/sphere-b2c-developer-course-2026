package com.lifestylehomecorp.customer.api;

import com.lifestylehomecorp.customer.api.dto.AddressRequest;
import com.lifestylehomecorp.customer.api.dto.AddressView;
import com.lifestylehomecorp.customer.api.dto.ChangePasswordRequest;
import com.lifestylehomecorp.customer.api.dto.CustomerView;
import com.lifestylehomecorp.customer.api.dto.EmailConfirmRequest;
import com.lifestylehomecorp.customer.api.dto.LoginRequest;
import com.lifestylehomecorp.customer.api.dto.MergeReportView;
import com.lifestylehomecorp.customer.api.dto.PasswordResetRequest;
import com.lifestylehomecorp.customer.api.dto.PasswordResetTokenRequest;
import com.lifestylehomecorp.customer.api.dto.SetGroupRequest;
import com.lifestylehomecorp.customer.api.dto.SignInView;
import com.lifestylehomecorp.customer.api.dto.SignUpRequest;
import com.lifestylehomecorp.customer.api.dto.TokenView;
import com.lifestylehomecorp.customer.api.dto.UpdateProfileRequest;
import com.lifestylehomecorp.customer.application.AddressInput;
import com.lifestylehomecorp.customer.application.CustomerService;
import com.lifestylehomecorp.platform.annotations.TaskDescription;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Identity HTTP contract (Session 5 — "Know Your Customer"). Every "me" endpoint resolves the customer
 * from the BFF-held {@code ShopperSession}, never from a request parameter — that IS the security
 * boundary the session teaches (5.9). Sign-up / sign-in carry the guest's anonymousId so the S4 basket
 * and wishlist follow the shopper; the merge mode (5.3) decides what happens when two baskets meet.
 */
@RestController
public class CustomerController {

    private final CustomerService customers;

    public CustomerController(CustomerService customers) {
        this.customers = customers;
    }

    // ---- 5.1 / 5.2 / 5.3 ---------------------------------------------------------------------

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 1,
            title = "Register (sign up)", tier = "T1", capability = "customer.register",
            description = "Implement CustomerRepository.signUp(...) in the customer module's infrastructure "
                    + "layer — POST a CustomerDraft (email + password + firstName + lastName, the store when "
                    + "given, and the session's anonymousId so the guest's carts / shopping lists / orders "
                    + "become the new customer's) and return the raw CustomerSignInResult (customer + cart). "
                    + "The service records the customer on the session and maps a PII-safe profile. "
                    + "POST /api/customers/signup drives the 'Create account' form; a duplicate email is a 409.",
            hint = "Docs: Customers — create (sign up) a Customer with a CustomerDraft; anonymousId "
                    + "(docs.commercetools.com/api/projects/customers#create-sign-up-customer).")
    @PostMapping("/api/customers/signup")
    public SignInView signUp(@RequestBody SignUpRequest req) {
        return SignInView.from(customers.signUp(req.email(), req.password(), req.firstName(), req.lastName(), req.store()));
    }

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 2,
            title = "Sign in (login)", tier = "T1", capability = "customer.login",
            description = "Implement CustomerRepository.signIn(...) in the customer module's infrastructure "
                    + "layer — POST /login with a CustomerSignin (email + password + the session's anonymousId + "
                    + "anonymousCartSignInMode) and return the raw CustomerSignInResult: the customer AND their "
                    + "RECALCULATED active cart. Sign-in authenticates and returns state — it is not a token; "
                    + "the BFF session holds who is signed in. POST /api/customers/login drives the sign-in "
                    + "form; wrong email or password → the same vague 400 (no user enumeration).",
            hint = "Docs: Customers — authenticate (sign in) a Customer; CustomerSignInResult "
                    + "(docs.commercetools.com/api/projects/customers#authenticate-sign-in-customer).")
    @PostMapping("/api/customers/login")
    public SignInView login(@RequestBody LoginRequest req) {
        return SignInView.from(customers.signIn(req.email(), req.password()));
    }

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 3,
            title = "Guest → customer cart merge", tier = "T2", capability = "customer.cartMerge",
            description = "Implement CustomerService.signInAndResolveCart(email, password, mergeMode) so sign-in carries the "
                    + "guest basket over: choose an AnonymousCartSignInMode (MergeWithExistingCustomerCart vs "
                    + "UseAsNewActiveCustomerCart), pass it with the session's anonymousId on the login call, "
                    + "re-point the session at the RETURNED cart (the guest cart may now be a read-only "
                    + "cartState=Merged cart), and build a merge report from it: lines merged (higher quantity "
                    + "wins), lines added, lines left behind (key conflicts). POST "
                    + "/api/customers/login?mergeMode=… signs in with an explicit mode; GET /api/cart/merge-report "
                    + "re-derives the report. A currency / store mismatch must never produce a half-merged cart.",
            hint = "Docs: Customers overview — cart merge during sign-in; Carts & Orders overview — merge rules "
                    + "(docs.commercetools.com/api/carts-orders-overview#merge-a-cart).",
            decisions = {
                    "Merge strategy — MergeWithExistingCustomerCart (keep both sessions) vs "
                            + "UseAsNewActiveCustomerCart (the guest basket is the intent); fixed, per-store, or shopper-chosen.",
                    "Ineligible carts (currency / store mismatch) — block with a reason vs fall back to UseAsNew… "
                            + "(a silent fallback loses items without telling anyone).",
                    "What the shopper is told — a count, the full diff, and where left-behind items go.",
                    "Built-in modes vs a custom merge — sign in WITHOUT anonymousId and replay addLineItem under your own rules."
            })
    @PostMapping(value = "/api/customers/login", params = "mergeMode")
    public SignInView loginWithMergeMode(@RequestBody LoginRequest req, @RequestParam String mergeMode) {
        return SignInView.from(customers.signInAndResolveCart(req.email(), req.password(), mergeMode));
    }

    /** 5.3 — re-derive the merge report for a given anonymous cart against the session's active cart. */
    @GetMapping("/api/cart/merge-report")
    public MergeReportView mergeReport(@RequestParam String anonymousCartId) {
        return MergeReportView.from(customers.mergeReport(anonymousCartId));
    }

    // ---- 5.4 / 5.5 / 5.6 ---------------------------------------------------------------------

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 4,
            title = "My profile: read & update", tier = "T1", capability = "customer.profile",
            description = "Pre-built — study it. GET /api/customers/me reads the signed-in customer (by the id "
                    + "on the BFF session, customerGroup expanded) and returns the PII-safe projection; PATCH "
                    + "/api/customers/me composes setFirstName / setLastName / changeEmail update actions "
                    + "carrying the customer's version — the S4 cart pattern on a second resource (it rides on "
                    + "task 5.5's update). changeEmail resets isEmailVerified.",
            hint = "Docs: Customers — get by ID; update actions setFirstName / changeEmail "
                    + "(docs.commercetools.com/api/projects/customers#update-actions).")
    @GetMapping("/api/customers/me")
    public CustomerView me() {
        return CustomerView.from(customers.profile());
    }

    @PatchMapping("/api/customers/me")
    public CustomerView updateMe(@RequestBody UpdateProfileRequest req) {
        return CustomerView.from(customers.updateProfile(req.firstName(), req.lastName(), req.email()));
    }

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 5,
            title = "Address book + defaults", tier = "T1", capability = "customer.addresses",
            description = "Implement CustomerRepository.update(customerId, version, actions) in the customer "
                    + "module's infrastructure layer — POST a CustomerUpdate carrying the customer's current "
                    + "version + a list of update actions, returning the raw SDK Customer (the twin of the cart's "
                    + "update). The service composes addAddress + setDefaultShippingAddress (by the address KEY, "
                    + "so both land in ONE update), removeAddress, and — via 5.4 / 5.6 — the name, email and group "
                    + "actions. POST /api/customers/me/addresses saves an address; the default pre-fills the "
                    + "cart's shipping address.",
            hint = "Docs: Customers — addAddress / setDefaultShippingAddress / removeAddress "
                    + "(docs.commercetools.com/api/projects/customers#add-address).")
    @PostMapping("/api/customers/me/addresses")
    public CustomerView addAddress(@RequestBody AddressRequest req) {
        return CustomerView.from(customers.addAddress(
                new AddressInput(req.key(), req.country(), req.firstName(), req.lastName(),
                        req.streetName(), req.streetNumber(), req.postalCode(), req.city()),
                Boolean.TRUE.equals(req.defaultShipping()),
                Boolean.TRUE.equals(req.defaultBilling())));
    }

    @GetMapping("/api/customers/me/addresses")
    public List<AddressView> addresses() {
        return customers.profile().addresses().stream().map(AddressView::from).toList();
    }

    @PutMapping("/api/customers/me/addresses/{addressId}/default")
    public CustomerView setDefaultShipping(@PathVariable String addressId) {
        return CustomerView.from(customers.setDefaultShippingAddress(addressId));
    }

    @DeleteMapping("/api/customers/me/addresses/{addressId}")
    public CustomerView removeAddress(@PathVariable String addressId) {
        return CustomerView.from(customers.removeAddress(addressId));
    }

    /** 5.6 (assign, pre-built) — a trainer/admin lever: put the customer in a group by key (e.g. "vip"). */
    @PutMapping("/api/customers/me/group")
    public CustomerView setGroup(@RequestBody SetGroupRequest req) {
        return CustomerView.from(customers.setCustomerGroup(req.customerGroupKey()));
    }

    // ---- 5.7 / 5.8 ------------------------------------------------------------------------------

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 7,
            title = "Password: change & reset", tier = "T1", capability = "customer.password",
            description = "Implement the three CustomerRepository password calls in the infrastructure layer: "
                    + "changePassword (POST /customers/password — id + version + currentPassword + newPassword), "
                    + "createPasswordResetToken (POST /customers/password-token — email + a SHORT ttlMinutes; in "
                    + "production the token value is EMAILED, never returned to the browser — the course has no "
                    + "mail service, so the BFF returns it with a demo note) and resetPassword (POST "
                    + "/customers/password/reset — tokenValue + newPassword; the token is the proof). Both flows "
                    + "invalidate every other token for the customer. POST /api/customers/me/password changes it; "
                    + "POST /api/customers/password/reset-token then /reset is 'Forgot password?'.",
            hint = "Docs: Customers overview — customer password reset; Customers — change password "
                    + "(docs.commercetools.com/api/customers-overview#customer-password-reset).")
    @PostMapping("/api/customers/me/password")
    public CustomerView changePassword(@RequestBody ChangePasswordRequest req) {
        return CustomerView.from(customers.changePassword(req.currentPassword(), req.newPassword()));
    }

    @PostMapping("/api/customers/password/reset-token")
    public TokenView requestPasswordReset(@RequestBody PasswordResetTokenRequest req) {
        return TokenView.from(customers.requestPasswordReset(req.email()));
    }

    @PostMapping("/api/customers/password/reset")
    public CustomerView resetPassword(@RequestBody PasswordResetRequest req) {
        return CustomerView.from(customers.resetPassword(req.tokenValue(), req.newPassword()));
    }

    /** 5.8 (pre-built) — issue a verification token for the signed-in customer (demo: returned, not emailed). */
    @PostMapping("/api/customers/email/token")
    public TokenView requestEmailVerification() {
        return TokenView.from(customers.requestEmailVerification());
    }

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 8,
            title = "Email verification", tier = "T1", capability = "customer.emailVerify",
            description = "Pre-built — study it. POST /api/customers/email/token issues a CustomerToken for the "
                    + "signed-in customer (POST /customers/email-token — id + version + ttlMinutes); POST "
                    + "/api/customers/email/confirm with its value confirms the email (POST "
                    + "/customers/email/confirm) → isEmailVerified = true. The same two-step token shape as 5.7. "
                    + "isEmailVerified is a BUSINESS flag, not an auth gate — commercetools signs in unverified "
                    + "customers; what verification gates is your policy.",
            hint = "Docs: Customers overview — customer email verification "
                    + "(docs.commercetools.com/api/customers-overview#customer-email-verification).")
    @PostMapping("/api/customers/email/confirm")
    public CustomerView confirmEmail(@RequestBody EmailConfirmRequest req) {
        return CustomerView.from(customers.confirmEmail(req.tokenValue()));
    }

    // ---- 5.10 -----------------------------------------------------------------------------------

    @TaskDescription(
            module = "customer", session = "Session 5", taskNumber = 10,
            title = "PII & GDPR: what crosses the boundary", tier = "T2", capability = "customer.pii",
            description = "Homework. Two halves: (1) the PII-safe projection — CustomerMapper.toProfile is the "
                    + "ONE place that decides what leaves the BFF (no password, customerNumber, externalId, custom "
                    + "fields; no email in log lines); (2) deletion — implement CustomerRepository.delete(id, "
                    + "version) (DELETE /customers/{id}?version=…) behind DELETE /api/customers/me, after the "
                    + "session check. Orders keep their customer snapshot and nothing cascades, so decide "
                    + "delete vs anonymise first. Afterwards the session is a fresh guest.",
            hint = "Docs: Customers — delete a Customer; GDPR / data erasure guidance "
                    + "(docs.commercetools.com/api/projects/customers#delete-customer).",
            decisions = {
                    "Erasure vs retention — orders reference the customer and must be kept for accounting; "
                            + "delete the Customer, or anonymise it (tombstone email, blank name, drop addresses)?",
                    "What crosses to the browser per surface — the header needs a first name; the account page "
                            + "needs the address book; nothing needs customerNumber or internal ids.",
                    "Logging + telemetry — emails in logs are PII, correlation ids are not; does the S3 "
                            + "request-body capture retain sign-in bodies?",
                    "Data scope — a store-specific customer's data belongs to that store's controller."
            })
    @DeleteMapping("/api/customers/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe() {
        customers.deleteAccount();
    }
}
