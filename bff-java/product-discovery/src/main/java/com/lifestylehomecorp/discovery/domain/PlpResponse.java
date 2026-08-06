package com.lifestylehomecorp.discovery.domain;

import java.util.List;

/**
 * The full result of a store-scoped Product Search: the hydrated cards, the computed facets, and
 * the paging window ({@code total} matched, {@code offset}/{@code limit} of this page). Produced by
 * ONE Product Search call — cards and facets together.
 */
public record PlpResponse(List<PlpCard> cards, List<Facet> facets, long total, int offset, int limit) {
}
