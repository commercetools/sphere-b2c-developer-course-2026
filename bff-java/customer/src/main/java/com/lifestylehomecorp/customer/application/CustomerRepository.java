package com.lifestylehomecorp.customer.application;

import com.commercetools.api.models.customer.AnonymousCartSignInMode;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.customer.CustomerToken;
import com.commercetools.api.models.customer.CustomerUpdateAction;

import java.util.List;

/**
 * The customer SDK gateway (implemented in infrastructure). Returns RAW SDK types; the
 * {@link CustomerService} maps to the domain and owns version + 409-retry around {@link #update}.
 * Participants implement the SDK call in each stubbed method (T1); {@link #get} and the email-token
 * pair are trainer-provided plumbing.
 */
public interface CustomerRepository {

    /** 5.1 — create (sign up) a customer; the anonymousId on the draft hands the guest's resources over. */
    CustomerSignInResult signUp(SignUpInput in);

    /**
     * 5.2 / 5.3 — authenticate (sign in). Returns the customer + their recalculated active cart; the
     * mode decides what happens when the guest cart meets an existing customer cart.
     */
    CustomerSignInResult signIn(String email, String password, String anonymousId, AnonymousCartSignInMode mode);

    /** Fetch the current customer (customerGroup expanded) — used before every update for the latest version. */
    Customer get(String customerId);

    /**
     * Plumbing for the merge preflight (5.3): the customer with this email, or null. A server-side lookup
     * (the BFF has the scope; a /me token could not do this) — never surfaced to the browser.
     */
    Customer findByEmail(String email);

    /** 5.5 — apply a batch of update actions with the customer's current version (profile, addresses, group). */
    Customer update(String customerId, Long version, List<CustomerUpdateAction> actions);

    /** 5.7 — change the password of a signed-in customer (needs the current password + version). */
    Customer changePassword(String customerId, Long version, String currentPassword, String newPassword);

    /** 5.7 — issue a password-reset token for the email (production: email it; never return it). */
    CustomerToken createPasswordResetToken(String email, long ttlMinutes);

    /** 5.7 — reset the password with a token; invalidates every other token for the customer. */
    Customer resetPassword(String tokenValue, String newPassword);

    /** 5.8 (pre-built) — issue an email-verification token for the customer. */
    CustomerToken createEmailToken(String customerId, Long version, long ttlMinutes);

    /** 5.8 (pre-built) — confirm the email with the token → isEmailVerified = true. */
    Customer confirmEmail(String tokenValue);

    /** 5.10 (homework) — delete the customer (orders keep their snapshot; nothing cascades). */
    Customer delete(String customerId, Long version);
}
