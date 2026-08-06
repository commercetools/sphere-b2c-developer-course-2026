package com.lifestylehomecorp.discovery.application;

/**
 * Resolves a store <em>key</em> to its commercetools <em>id</em>. Product Search scopes to stores
 * by <b>id</b> (the {@code stores} keyword field holds store ids), but the storefront works in keys,
 * so the BFF resolves once and caches — the same pattern as {@link ChannelRepository}.
 */
public interface StoreRepository {

    /** The store id for the given key, or {@code null} if the key is null/blank or unknown. */
    String idByKey(String key);
}
