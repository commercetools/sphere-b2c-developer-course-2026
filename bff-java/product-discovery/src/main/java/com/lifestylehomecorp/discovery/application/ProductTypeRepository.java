package com.lifestylehomecorp.discovery.application;

import com.commercetools.api.models.product_type.ProductType;

import java.util.List;

/**
 * Reads the project's {@code ProductType} definitions — the source of truth for which attributes a
 * product carries and whether each is <b>searchable</b>. Task 3.8 ({@link SearchService#facetConfig})
 * derives the facet set from these instead of hardcoding it, so adding a searchable attribute in the
 * Merchant Center surfaces a filter with no code change.
 *
 * <p>Returns the RAW SDK {@link ProductType} (SDK types are confined to {@code infrastructure} +
 * {@code application}); the service inspects the {@code AttributeDefinition}s and maps them to
 * SDK-free {@link FacetSpec}s.
 */
public interface ProductTypeRepository {

    /** All product types in the project (few and stable — read once, map in the service). */
    List<ProductType> findAll();
}
