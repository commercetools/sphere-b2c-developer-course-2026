package com.lifestylehomecorp.project.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.models.store.Store;
import com.lifestylehomecorp.project.application.StoreRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SDK gateway for Stores — the ONE place participants write commercetools SDK code for the Session 1
 * store tasks. Each method is *only* the SDK fluent call, returning raw SDK types; the mapping to
 * domain models happens in {@link com.lifestylehomecorp.project.application.StoreService}.
 *
 * <p>Compare against {@link CtProjectRepository} for the pattern. Ground every call on the
 * {@code commercetools-knowledge} MCP.
 */
@Repository
public class CtStoreRepository implements StoreRepository {

    private final ProjectApiRoot apiRoot;

    public CtStoreRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public List<Store> findAll() {
        // Query Stores. Expand distributionChannels so the mapper can read channel KEYS (a reference
        // carries only the id by default). The paged response wraps the list in getResults().
        return apiRoot.stores()
                .get()
                .withExpand("distributionChannels[*]")
                .executeBlocking()
                .getBody()
                .getResults();
    }

    @Override
    public Store findByKey(String key) {
        // Fetch one Store BY KEY (not id). Expand distributionChannels[*] so the mapper reads channel
        // KEYS, matching findAll(). A commercetools 404 surfaces as a NotFoundException, which the
        // platform error advice maps to a clean HTTP 404 — no SDK exception leaks past this layer.
        return apiRoot.stores()
                .withKey(key)
                .get()
                .withExpand("distributionChannels[*]")
                .executeBlocking()
                .getBody();
    }
}
