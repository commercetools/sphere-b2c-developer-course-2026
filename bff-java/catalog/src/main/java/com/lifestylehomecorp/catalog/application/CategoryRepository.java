package com.lifestylehomecorp.catalog.application;

import com.commercetools.api.models.category.Category;

import java.util.List;

/** SDK gateway for categories — returns raw SDK types; {@link CatalogService} maps to the domain. */
public interface CategoryRepository {

    /** Task 2.3 (T1) — the SDK call to list categories. */
    List<Category> findAll();
}
