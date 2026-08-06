package com.lifestylehomecorp.discovery.infrastructure;

import com.commercetools.api.models.graph_ql.GraphQLResponse;
import com.commercetools.api.client.ProjectApiRoot;
import com.lifestylehomecorp.discovery.application.SearchRepository;
import com.lifestylehomecorp.discovery.application.SearchRequest;
import org.springframework.stereotype.Repository;

/**
 * SDK gateway for the Product Search API. The ONE place SDK code is written for Session 3: build the
 * {@code productsSearch} GraphQL document (via {@link ProductSearchDocument}) and POST it through
 * {@code apiRoot.graphql().post(...)}, returning the RAW {@link GraphQLResponse}. Store scope,
 * full-text/category filtering, facets, sort, pagination AND per-hit hydration (name/slug/price/image)
 * all resolve in this single call — no N+1 and no second retrieval API.
 *
 * <p>The document is assembled by {@link ProductSearchDocument} as an immutable {@link SearchExpr} tree
 * passed as typed GraphQL variables (the query text stays constant — no caller value is concatenated
 * into it), so this repository is just the SDK call.
 */
@Repository
public class CtSearchRepository implements SearchRepository {

    private final ProjectApiRoot apiRoot;

    public CtSearchRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public GraphQLResponse search(SearchRequest request) {
        return apiRoot.graphql()
                .post(ProductSearchDocument.build(request))
                .executeBlocking()
                .getBody();
    }
}
