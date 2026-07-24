package com.lifestylehomecorp.catalog.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.category.Category;
import com.lifestylehomecorp.catalog.application.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for categories — a participant TODO. Only the SDK call; mapping is in the service.
 * See {@link CtProductRepository} for the pattern.
 */
@Repository
public class CtCategoryRepository implements CategoryRepository {

    private final ProjectApiRoot apiRoot;

    public CtCategoryRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public List<Category> findAll() {
        // List categories for the storefront navigation. limit pages the result SERVER-SIDE; the
        // paged response wraps the list in getResults().
        return apiRoot.categories()
                .get()
                .withLimit(100)
                .executeBlocking()
                .getBody()
                .getResults();
    }
}
