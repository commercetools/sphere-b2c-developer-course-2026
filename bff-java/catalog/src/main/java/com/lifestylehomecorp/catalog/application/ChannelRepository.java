package com.lifestylehomecorp.catalog.application;

/**
 * Resolves a distribution-channel <em>key</em> to its commercetools <em>id</em>. Price selection is
 * id-based ({@code priceChannel} expects a channel id), but the storefront works in keys (the
 * "prefer key over id" convention), so the BFF resolves once and caches — the pattern the
 * commercetools docs recommend for channel-scoped price selection.
 */
public interface ChannelRepository {

    /**
     * The channel id for the given key, or {@code null} if the key is null/blank or no such channel
     * exists (a missing channel must not break a catalog read — price selection just omits it).
     */
    String idByKey(String key);
}
