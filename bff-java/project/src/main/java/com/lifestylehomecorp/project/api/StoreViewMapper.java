package com.lifestylehomecorp.project.api;

import com.lifestylehomecorp.project.api.dto.StoreContextView;
import com.lifestylehomecorp.project.api.dto.StoreView;
import com.lifestylehomecorp.project.domain.StoreContext;
import com.lifestylehomecorp.project.domain.StoreSummary;

/** Maps store domain records to their wire view models. */
public final class StoreViewMapper {

    private StoreViewMapper() {
    }

    public static StoreView toView(StoreSummary s) {
        return new StoreView(s.key(), s.name(), s.languages(), s.countries(), s.channelKeys());
    }

    public static StoreContextView toView(StoreContext c) {
        return new StoreContextView(
                c.activeStoreKey(), c.activeStoreName(), c.availableStoreKeys(),
                c.languages(), c.countries(), c.distributionChannelKeys());
    }
}
