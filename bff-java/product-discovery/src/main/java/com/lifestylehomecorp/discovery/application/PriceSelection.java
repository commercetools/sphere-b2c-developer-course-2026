package com.lifestylehomecorp.discovery.application;

/**
 * The price-selection context for a discovery read — the parameters that pick the shopper's
 * best-matching price on each hydrated card. Mirrors the catalog module's {@code PriceSelection}:
 * {@code currency}/{@code country} are the codes from the client; {@code channel}/{@code customerGroup}
 * are commercetools <b>ids</b> at the repository boundary (the client sends a channel <em>key</em>,
 * resolved to an id by {@link SearchService} via {@link ChannelRepository}). Any field may be
 * {@code null}/blank, in which case it is omitted from the Product Search {@code price(...)} selector.
 */
public record PriceSelection(String currency, String country, String channel, String customerGroup) {

    /** An empty selection (no price parameters). */
    public static PriceSelection none() {
        return new PriceSelection(null, null, null, null);
    }

    /** A copy with the channel replaced (used when resolving a channel key to its id). */
    public PriceSelection withChannel(String channelId) {
        return new PriceSelection(currency, country, channelId, customerGroup);
    }
}
