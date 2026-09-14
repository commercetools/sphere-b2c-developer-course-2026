package com.lifestylehomecorp.customer.application;

import com.commercetools.api.models.customer.Customer;
import com.lifestylehomecorp.customer.domain.PriceContext;
import com.lifestylehomecorp.customer.domain.ShopperContext;
import com.lifestylehomecorp.platform.session.ShopperSession;
import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import org.springframework.stereotype.Service;

/**
 * The identity boundary (5.9): projects the BFF-held {@link ShopperSession} into what the browser may
 * know, and exposes the resolved price context (5.6). Identity is never taken from a request — the
 * session is the single source, which is what lets the BFF enforce rules a {@code /me} token cannot.
 */
@Service
public class SessionService {

    private final ShopperSession session;
    private final CustomerRepository customers;

    public SessionService(ShopperSession session, CustomerRepository customers) {
        this.session = session;
        this.customers = customers;
    }

    /** 5.9 — who is signed in, for the header: a flag, a first name, a pricing tier. No ids. */
    public ShopperContext current() {
        // TODO (Task 5.9): project the BFF-held ShopperSession into what the browser may know — signedIn, a
        // first name (customers.get(session.requireCustomerId())), the customer-group KEY — and NOTHING more.
        // Identity comes from the session, never from a request parameter: that is the boundary this task
        // teaches. Decisions you own are in the @TaskDescription; see session-tasks-detailed.md § 5.9.
        throw new TaskNotImplementedException("5.9");
    }

    /**
     * 5.6 — the price-selection tuple as the catalogue will apply it for THIS shopper. The group comes
     * from the session (set at sign-in / by setCustomerGroup); a guest resolves to no group — never a
     * default one — so the ungrouped price is the guest price.
     */
    public PriceContext priceContext(String currency, String country, String channel) {
        // TODO (Task 5.6): return the price-selection tuple the catalogue applies for THIS shopper — the
        // currency / country / channel from the request plus the customer group from the session
        // (session.customerGroupIdOrNull(); resolve its KEY via customers.get for display). A guest resolves to
        // NO group — never a default one. Decisions you own are in the @TaskDescription; § 5.6.
        throw new TaskNotImplementedException("5.6");
    }

    /** Sign out: drop the customer + cart and mint a fresh anonymousId (the old one was a signed-in shopper's). */
    public ShopperContext signOut() {
        session.signOut();
        return ShopperContext.guest();
    }
}
