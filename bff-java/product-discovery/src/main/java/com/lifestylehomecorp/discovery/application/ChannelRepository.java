package com.lifestylehomecorp.discovery.application;

/**
 * Resolves a distribution-channel <em>key</em> to its commercetools <em>id</em>. Product Search
 * price selection is id-based ({@code channelId} on the {@code price(...)} selector), but the
 * storefront works in keys, so the BFF resolves once and caches. (A copy of the catalog module's
 * resolver — {@code product-discovery} depends on {@code platform} only, never on {@code catalog}.)
 */
public interface ChannelRepository {

    /** The channel id for the given key, or {@code null} if the key is null/blank or unknown. */
    String idByKey(String key);
}
