package com.lifestylehomecorp.customer.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.customer.AnonymousCartSignInMode;
import com.commercetools.api.models.customer.Customer;
import com.commercetools.api.models.customer.CustomerChangePasswordBuilder;
import com.commercetools.api.models.customer.CustomerCreateEmailTokenBuilder;
import com.commercetools.api.models.customer.CustomerCreatePasswordResetTokenBuilder;
import com.commercetools.api.models.customer.CustomerDraftBuilder;
import com.commercetools.api.models.customer.CustomerEmailVerifyBuilder;
import com.commercetools.api.models.customer.CustomerResetPasswordBuilder;
import com.commercetools.api.models.customer.CustomerSignInResult;
import com.commercetools.api.models.customer.CustomerSigninBuilder;
import com.commercetools.api.models.customer.CustomerToken;
import com.commercetools.api.models.customer.CustomerUpdateAction;
import com.commercetools.api.models.customer.CustomerUpdateBuilder;
import com.commercetools.api.models.store.StoreResourceIdentifierBuilder;
import com.lifestylehomecorp.customer.application.CustomerRepository;
import com.lifestylehomecorp.customer.application.SignUpInput;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for the Customer — the ONE place SDK code is written for Session 5. Each method is a
 * single commercetools call returning the raw SDK type; {@link com.lifestylehomecorp.customer.application.CustomerService}
 * owns version + 409-retry and the PII-safe mapping.
 *
 * <p>This is the SOLUTION (answer key): every task method carries its SDK call. On the starter branch
 * 5.1 / 5.2 / 5.5 / 5.7 / 5.10 throw {@code TaskNotImplementedException}; {@link #get},
 * {@link #createEmailToken} and {@link #confirmEmail} are trainer-provided plumbing on both.
 */
@Repository
public class CtCustomerRepository implements CustomerRepository {

    /** Expand the group so the profile can show its KEY ("vip") — a reference carries only the id. */
    private static final String EXPAND_GROUP = "customerGroup";

    private final ProjectApiRoot apiRoot;

    public CtCustomerRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    /**
     * 5.1 — POST /customers with a CustomerDraft. The guest's {@code anonymousId} on the draft hands every
     * cart / shopping list / order / payment carrying it to the new customer; a brand-new customer has no
     * other cart, so a sign-in mode would be moot (the guest cart simply becomes theirs). The store (when
     * given) makes a store-specific customer; the general endpoint accepts {@code stores} on the draft.
     */
    @Override
    public CustomerSignInResult signUp(SignUpInput in) {
        CustomerDraftBuilder draft = CustomerDraftBuilder.of()
                .email(in.email())
                .password(in.password())
                .firstName(in.firstName())
                .lastName(in.lastName())
                .anonymousId(in.anonymousId()); // no sign-in mode on the draft in SDK 19.11 — moot for a new customer
        if (in.storeKey() != null && !in.storeKey().isBlank()) {
            draft.stores(StoreResourceIdentifierBuilder.of().key(in.storeKey()).build());
        }
        return apiRoot.customers().post(draft.build()).executeBlocking().getBody();
    }

    /**
     * 5.2 / 5.3 — POST /login with a CustomerSignin. Authenticates and returns STATE: the customer plus
     * their most recently modified active cart, recalculated. {@code anonymousId} assigns the guest's
     * resources; {@code anonymousCartSignInMode} (5.3) decides how the guest cart meets an existing
     * customer cart. Wrong email or password → the same 400 InvalidCredentials (passed through as-is).
     */
    @Override
    public CustomerSignInResult signIn(String email, String password, String anonymousId, AnonymousCartSignInMode mode) {
        return apiRoot.login()
                .post(CustomerSigninBuilder.of()
                        .email(email)
                        .password(password)
                        .anonymousId(anonymousId)
                        .anonymousCartSignInMode(mode)
                        .build())
                .executeBlocking().getBody();
    }

    @Override
    public Customer get(String customerId) {
        return apiRoot.customers().withId(customerId).get()
                .withExpand(EXPAND_GROUP)
                .executeBlocking().getBody();
    }

    /** 5.5 — POST /customers/{id} with a CustomerUpdate (version + actions): the cart's update, on a second resource. */
    @Override
    public Customer findByEmail(String email) {
        List<Customer> results = apiRoot.customers().get()
                .withWhere("lowercaseEmail = :email")
                .withPredicateVar("email", email == null ? "" : email.toLowerCase())
                .withLimit(1)
                .executeBlocking().getBody().getResults();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Customer update(String customerId, Long version, List<CustomerUpdateAction> actions) {
        return apiRoot.customers().withId(customerId)
                .post(CustomerUpdateBuilder.of().version(version).actions(actions).build())
                .withExpand(EXPAND_GROUP)
                .executeBlocking().getBody();
    }

    /** 5.7 — POST /customers/password: the current password AND the version are required; other tokens are invalidated. */
    @Override
    public Customer changePassword(String customerId, Long version, String currentPassword, String newPassword) {
        return apiRoot.customers().password()
                .post(CustomerChangePasswordBuilder.of()
                        .id(customerId)
                        .version(version)
                        .currentPassword(currentPassword)
                        .newPassword(newPassword)
                        .build())
                .executeBlocking().getBody();
    }

    /**
     * 5.7 — POST /customers/password-token. The token is a secret in transit: in production its value is
     * EMAILED to the customer; the service only returns it because the course has no mail service. A
     * short TTL and {@code invalidateOlderTokens} keep a re-request from leaving live links around.
     */
    @Override
    public CustomerToken createPasswordResetToken(String email, long ttlMinutes) {
        return apiRoot.customers().passwordToken()
                .post(CustomerCreatePasswordResetTokenBuilder.of()
                        .email(email)
                        .ttlMinutes(ttlMinutes)
                        .invalidateOlderTokens(true)
                        .build())
                .executeBlocking().getBody();
    }

    /** 5.7 — POST /customers/password/reset: no version, no session — the token is the proof. */
    @Override
    public Customer resetPassword(String tokenValue, String newPassword) {
        return apiRoot.customers().passwordReset()
                .post(CustomerResetPasswordBuilder.of()
                        .tokenValue(tokenValue)
                        .newPassword(newPassword)
                        .build())
                .executeBlocking().getBody();
    }

    @Override
    public CustomerToken createEmailToken(String customerId, Long version, long ttlMinutes) {
        return apiRoot.customers().emailToken()
                .post(CustomerCreateEmailTokenBuilder.of()
                        .id(customerId)
                        .version(version)
                        .ttlMinutes(ttlMinutes)
                        .build())
                .executeBlocking().getBody();
    }

    @Override
    public Customer confirmEmail(String tokenValue) {
        return apiRoot.customers().emailConfirm()
                .post(CustomerEmailVerifyBuilder.of().tokenValue(tokenValue).build())
                .executeBlocking().getBody();
    }

    /** 5.10 — DELETE /customers/{id}?version=… Orders keep their customer snapshot; nothing cascades. */
    @Override
    public Customer delete(String customerId, Long version) {
        return apiRoot.customers().withId(customerId)
                .delete()
                .withVersion(version)
                .executeBlocking().getBody();
    }
}
