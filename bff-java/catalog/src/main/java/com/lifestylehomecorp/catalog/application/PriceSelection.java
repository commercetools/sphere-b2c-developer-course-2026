package com.lifestylehomecorp.catalog.application;

/**
 * The price-selection context for a catalog read — the commercetools price-selection parameters that
 * pick the best-matching embedded price per variant. Passing all applicable parameters lets the
 * platform apply its precedence (Customer Group &gt; Channel &gt; country) and fall back to the base
 * currency price when a more specific price does not exist.
 *
 * <p>{@code currency} and {@code country} are the codes as received from the client. {@code channel}
 * and {@code customerGroup} are commercetools <b>ids</b> at the repository boundary (price selection
 * is id-based): the client sends a channel <em>key</em>, and {@link CatalogService} resolves it to an
 * id via {@link ChannelRepository} before the read. Any field may be {@code null}/blank, in which
 * case that parameter is simply omitted from the SDK call.
 */
public record PriceSelection(String currency, String country, String channel, String customerGroup) {

    /** An empty selection (no price parameters) — e.g. for reads that don't need a selected price. */
    public static PriceSelection none() {
        return new PriceSelection(null, null, null, null);
    }

    /** A copy with the channel replaced (used when resolving a channel key to its id). */
    public PriceSelection withChannel(String channelId) {
        return new PriceSelection(currency, country, channelId, customerGroup);
    }
}
