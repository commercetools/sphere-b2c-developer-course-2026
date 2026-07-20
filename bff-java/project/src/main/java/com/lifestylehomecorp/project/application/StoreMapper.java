package com.lifestylehomecorp.project.application;

import com.commercetools.api.models.common.LocalizedString;
import com.commercetools.api.models.store.Store;
import com.commercetools.api.models.store_country.StoreCountry;
import com.lifestylehomecorp.project.domain.StoreSummary;

import java.util.List;

/**
 * Maps the commercetools SDK {@link Store} to the domain {@link StoreSummary}. This is the boundary
 * where SDK types stop — it lives in the service layer (trainer-provided; participants don't touch
 * it), so the T1 tasks can focus purely on the SDK call in the repository.
 */
final class StoreMapper {

    private StoreMapper() {
    }

    static StoreSummary toSummary(Store store) {
        List<String> channelKeys = store.getDistributionChannels() == null ? List.of()
                : store.getDistributionChannels().stream()
                        .map(ref -> ref.getObj() != null ? ref.getObj().getKey() : ref.getId())
                        .toList();
        List<String> countries = store.getCountries() == null ? List.of()
                : store.getCountries().stream().map(StoreCountry::getCode).toList();
        return new StoreSummary(
                store.getKey(),
                resolveName(store),
                store.getLanguages() == null ? List.of() : store.getLanguages(),
                countries,
                channelKeys);
    }

    /** Resolve the localized store name against the store's own languages, with a fallback. */
    private static String resolveName(Store store) {
        LocalizedString name = store.getName();
        if (name == null) {
            return store.getKey();
        }
        if (store.getLanguages() != null) {
            for (String language : store.getLanguages()) {
                String value = name.get(language);
                if (value != null) {
                    return value;
                }
            }
        }
        return name.values().values().stream().findFirst().orElse(store.getKey());
    }
}
