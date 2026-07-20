package com.lifestylehomecorp.project.application;

import com.commercetools.api.models.store.Store;

import java.util.List;

/**
 * SDK gateway for Stores. Deliberately returns **raw commercetools SDK types** — the repository
 * (its implementation in {@code infrastructure}) is the ONE place participants write SDK code, and
 * it contains *only* the SDK call. All mapping/business logic lives in {@link StoreService}.
 *
 * <p>Because it speaks SDK types, this port lives in {@code application} (next to the service that
 * owns it), keeping the {@code domain} package SDK-free.
 */
public interface StoreRepository {

    /** Task 1.2 (T1) — the SDK call to list Stores. */
    List<Store> findAll();

    /** Task 1.3 (T1) — the SDK call to fetch one Store by key. */
    Store findByKey(String key);
}
