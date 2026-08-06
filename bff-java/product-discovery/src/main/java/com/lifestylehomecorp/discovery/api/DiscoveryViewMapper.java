package com.lifestylehomecorp.discovery.api;

import com.lifestylehomecorp.discovery.api.dto.CardView;
import com.lifestylehomecorp.discovery.api.dto.FacetBucketView;
import com.lifestylehomecorp.discovery.api.dto.FacetView;
import com.lifestylehomecorp.discovery.api.dto.MoneyView;
import com.lifestylehomecorp.discovery.api.dto.PlpView;
import com.lifestylehomecorp.discovery.api.dto.PriceStatsView;
import com.lifestylehomecorp.discovery.domain.Facet;
import com.lifestylehomecorp.discovery.domain.Money;
import com.lifestylehomecorp.discovery.domain.PlpCard;
import com.lifestylehomecorp.discovery.domain.PlpResponse;
import com.lifestylehomecorp.discovery.domain.PriceStats;

/**
 * Maps discovery domain records to their wire view models. The single translation point between the
 * domain and the HTTP contract.
 */
public final class DiscoveryViewMapper {

    private DiscoveryViewMapper() {
    }

    public static CardView toView(PlpCard c) {
        return new CardView(c.key(), c.name(), c.slug(), toView(c.price()), toView(c.originalPrice()), c.imageUrl());
    }

    public static PlpView toView(PlpResponse r) {
        return new PlpView(
                r.cards().stream().map(DiscoveryViewMapper::toView).toList(),
                r.facets().stream().map(DiscoveryViewMapper::toView).toList(),
                r.total(), r.offset(), r.limit());
    }

    public static FacetView toView(Facet f) {
        return new FacetView(
                f.name(),
                f.type(),
                f.buckets() == null ? null
                        : f.buckets().stream()
                                .map(b -> new FacetBucketView(b.key(), b.count())).toList(),
                toView(f.stats()));
    }

    private static PriceStatsView toView(PriceStats s) {
        return s == null ? null : new PriceStatsView(s.min(), s.max(), s.mean());
    }

    private static MoneyView toView(Money money) {
        return money == null ? null : new MoneyView(money.currencyCode(), money.centAmount());
    }
}
