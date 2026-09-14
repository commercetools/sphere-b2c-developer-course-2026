package com.lifestylehomecorp.customer.application;

import com.commercetools.api.models.customer.Customer;
import com.lifestylehomecorp.customer.domain.PriceContext;
import com.lifestylehomecorp.customer.domain.ShopperContext;
import com.lifestylehomecorp.platform.session.ShopperSession;
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
        if (!session.isSignedIn()) {
            return ShopperContext.guest();
        }
        Customer c = customers.get(session.requireCustomerId());
        return new ShopperContext(true, c.getFirstName(), CustomerMapper.customerGroupKey(c));
    }

    /**
     * 5.6 — the price-selection tuple as the catalogue will apply it for THIS shopper. The group comes
     * from the session (set at sign-in / by setCustomerGroup); a guest resolves to no group — never a
     * default one — so the ungrouped price is the guest price.
     */
    public PriceContext priceContext(String currency, String country, String channel) {
        String groupId = session.customerGroupIdOrNull();
        String groupKey = null;
        if (groupId != null && session.isSignedIn()) {
            groupKey = CustomerMapper.customerGroupKey(customers.get(session.requireCustomerId()));
        }
        return new PriceContext(currency, country, channel, groupId, groupKey, session.isSignedIn());
    }

    /** Sign out: drop the customer + cart and mint a fresh anonymousId (the old one was a signed-in shopper's). */
    public ShopperContext signOut() {
        session.signOut();
        return ShopperContext.guest();
    }
}
