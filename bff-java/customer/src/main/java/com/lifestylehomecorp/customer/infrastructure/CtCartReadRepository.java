package com.lifestylehomecorp.customer.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.cart.Cart;
import com.lifestylehomecorp.customer.application.CartReadRepository;
import io.vrap.rmf.base.client.ApiHttpException;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Trainer-provided plumbing: the read the merge report (5.3) needs, owned by this module (no `cart` dependency). */
@Repository
public class CtCartReadRepository implements CartReadRepository {

    private final ProjectApiRoot apiRoot;

    public CtCartReadRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public Cart activeCartOfCustomer(String customerId) {
        List<Cart> results = apiRoot.carts().get()
                .withWhere("customerId = :cid and cartState = \"Active\"")
                .withPredicateVar("cid", customerId)
                .withSort("lastModifiedAt desc")
                .withLimit(1)
                .executeBlocking().getBody().getResults();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public Cart get(String cartId) {
        try {
            return apiRoot.carts().withId(cartId).get().executeBlocking().getBody();
        } catch (ApiHttpException ex) {
            if (ex.getStatusCode() == 404) {
                return null; // deleted after the merge — nothing to report on
            }
            throw ex;
        }
    }
}
