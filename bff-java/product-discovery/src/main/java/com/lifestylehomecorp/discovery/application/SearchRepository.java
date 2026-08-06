package com.lifestylehomecorp.discovery.application;

import com.commercetools.api.models.graph_ql.GraphQLResponse;

/**
 * SDK gateway for discovery. The ONE place commercetools SDK code is written for Session 3: it
 * builds the Product Search GraphQL document from a {@link SearchRequest} and posts it via
 * {@code apiRoot.graphql().post(...)}, returning the RAW {@link GraphQLResponse}. Parsing
 * {@code getData()} into the domain happens in {@link SearchService} / {@code SearchMapper} — SDK
 * types are confined to {@code infrastructure} + {@code application}, keeping {@code domain} and
 * {@code api} SDK-free.
 *
 * <p>Uses the <b>Product Search API</b> (not Product Projections): a single call that scopes to the
 * store, filters by full-text / category, computes facets, sorts and pages, AND hydrates each hit's
 * catalog Product (name/slug/price/image) via the {@code product { ... }} sub-selection — no N+1.
 */
public interface SearchRepository {

    /** Run one Product Search for the given request; returns the raw GraphQL response. */
    GraphQLResponse search(SearchRequest request);
}
