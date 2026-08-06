package com.lifestylehomecorp.discovery.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.lifestylehomecorp.discovery.application.StoreRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SDK-backed store key→id resolver with a process-lifetime cache. Product Search scopes to stores by
 * id, but the storefront works in keys; stores are few and stable, so one Stores read per distinct
 * key (then cached) keeps the scoped search a single Product Search call on the hot path. A missing
 * store resolves to {@code null} rather than throwing.
 *
 * <p>Explicit bean name: the aggregator scans every module, and {@code project} carries an
 * identically-named {@code CtStoreRepository}; a distinct bean name avoids the duplicate-name clash
 * (the two implement different module-local {@code StoreRepository} interfaces, so injection stays
 * unambiguous by type).
 */
@Repository("discoveryStoreRepository")
public class CtStoreRepository implements StoreRepository {

    private final ProjectApiRoot apiRoot;
    private final ConcurrentHashMap<String, String> idByKey = new ConcurrentHashMap<>();

    public CtStoreRepository(ProjectApiRoot apiRoot) {
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
            return apiRoot.stores().withKey(key).get().executeBlocking().getBody().getId();
        } catch (Exception e) {
            return null;
        }
    }
}
