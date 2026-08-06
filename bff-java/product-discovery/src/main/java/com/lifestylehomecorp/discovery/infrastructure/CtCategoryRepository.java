package com.lifestylehomecorp.discovery.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.lifestylehomecorp.discovery.application.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SDK-backed category key→id resolver with a process-lifetime cache. Product Search filters a category
 * subtree by the category id, but the storefront works in keys; categories are few and stable, so one
 * Categories read per distinct key (then cached) keeps the scoped search a single Product Search call on
 * the hot path. A missing category resolves to {@code null} rather than throwing.
 *
 * <p>Explicit bean name: the aggregator scans every module, and {@code catalog} carries an
 * identically-named {@code CtCategoryRepository}; a distinct bean name avoids the duplicate-name clash
 * (the two implement different module-local {@code CategoryRepository} interfaces, so injection stays
 * unambiguous by type).
 */
@Repository("discoveryCategoryRepository")
public class CtCategoryRepository implements CategoryRepository {

    private final ProjectApiRoot apiRoot;
    private final ConcurrentHashMap<String, String> idByKey = new ConcurrentHashMap<>();

    public CtCategoryRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public String idByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return idByKey.computeIfAbsent(key, this::lookup);
    }

    private String lookup(String key) {
        try {
            return apiRoot.categories().withKey(key).get().executeBlocking().getBody().getId();
        } catch (Exception e) {
            return null;
        }
    }
}
