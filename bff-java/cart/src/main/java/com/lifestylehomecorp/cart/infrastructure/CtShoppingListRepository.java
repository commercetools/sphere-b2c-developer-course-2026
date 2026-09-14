package com.lifestylehomecorp.cart.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.common.LocalizedStringBuilder;
import com.commercetools.api.models.shopping_list.ShoppingList;
import com.commercetools.api.models.shopping_list.ShoppingListDraftBuilder;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateAction;
import com.commercetools.api.models.shopping_list.ShoppingListUpdateBuilder;
import com.lifestylehomecorp.cart.application.ShoppingListRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for the guest shopping list. Reads expand {@code lineItems[*].variant} so each saved
 * line carries its SKU (needed to re-add it to the cart). B2C guest scope uses anonymousId — no
 * as-associate chain (that is the B2B pattern).
 */
@Repository
public class CtShoppingListRepository implements ShoppingListRepository {

    private static final String EXPAND_VARIANTS = "lineItems[*].variant";

    private final ProjectApiRoot apiRoot;

    public CtShoppingListRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public ShoppingList getByAnonymousId(String anonymousId) {
        List<ShoppingList> results = apiRoot.shoppingLists().get()
                .withWhere("anonymousId = :aid")
                .withPredicateVar("aid", anonymousId)
                .withExpand(EXPAND_VARIANTS)
                .withLimit(1)
                .executeBlocking().getBody().getResults();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public ShoppingList create(String anonymousId) {
        return apiRoot.shoppingLists()
                .post(ShoppingListDraftBuilder.of()
                        .name(LocalizedStringBuilder.of()
                                .addValue("en-GB", "Saved items")
                                .addValue("en-US", "Saved items")
                                .addValue("de-DE", "Gemerkte Artikel")
                                .build())
                        .anonymousId(anonymousId)
                        .build())
                .executeBlocking().getBody();
    }

    @Override
    public ShoppingList getByCustomerId(String customerId) {
        List<ShoppingList> results = apiRoot.shoppingLists().get()
                .withWhere("customer(id = :cid)")
                .withPredicateVar("cid", customerId)
                .withExpand(EXPAND_VARIANTS)
                .withLimit(1)
                .executeBlocking().getBody().getResults();
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public ShoppingList createForCustomer(String customerId) {
        return apiRoot.shoppingLists()
                .post(ShoppingListDraftBuilder.of()
                        .name(LocalizedStringBuilder.of()
                                .addValue("en-GB", "Saved items")
                                .addValue("en-US", "Saved items")
                                .addValue("de-DE", "Gemerkte Artikel")
                                .build())
                        .customer(cr -> cr.id(customerId))
                        .build())
                .executeBlocking().getBody();
    }

    @Override
    public ShoppingList update(String listId, Long version, List<ShoppingListUpdateAction> actions) {
        return apiRoot.shoppingLists().withId(listId)
                .post(ShoppingListUpdateBuilder.of().version(version).actions(actions).build())
                .withExpand(EXPAND_VARIANTS)
                .executeBlocking().getBody();
    }
}
