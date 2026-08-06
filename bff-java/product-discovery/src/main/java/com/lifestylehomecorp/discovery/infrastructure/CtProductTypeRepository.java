package com.lifestylehomecorp.discovery.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.product_type.ProductType;
import com.lifestylehomecorp.discovery.application.ProductTypeRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK-backed {@code ProductType} read for Task 3.8's dynamic facet derivation: one Product Types
 * query returning the raw SDK {@link ProductType}s. The service reads each type's searchable
 * {@code AttributeDefinition}s and maps them to facet specs — no SDK types leak past
 * {@code application}.
 *
 * <p>Explicit bean name: the aggregator scans every module; a distinct bean name keeps this from
 * clashing with any identically-named {@code CtProductTypeRepository} in another module (the two
 * would implement different module-local {@code ProductTypeRepository} interfaces, so injection stays
 * unambiguous by type — same guard the store/channel resolvers use).
 */
@Repository("discoveryProductTypeRepository")
public class CtProductTypeRepository implements ProductTypeRepository {

    private final ProjectApiRoot apiRoot;

    public CtProductTypeRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public List<ProductType> findAll() {
        return apiRoot.productTypes().get().executeBlocking().getBody().getResults();
    }
}
