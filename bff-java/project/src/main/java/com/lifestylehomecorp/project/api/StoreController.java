package com.lifestylehomecorp.project.api;

import com.lifestylehomecorp.platform.annotations.TaskDescription;
import com.lifestylehomecorp.project.api.dto.StoreContextView;
import com.lifestylehomecorp.project.api.dto.StoreView;
import com.lifestylehomecorp.project.application.StoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Session 1 store endpoints — introduced at a high level to teach SDK GET mechanics and the
 * platform anatomy (Project → Stores → Channels). api/application/domain are wired; only the
 * infrastructure adapter methods are stubbed.
 */
@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @TaskDescription(
            module = "project", session = "Session 1", taskNumber = 2,
            title = "List stores", tier = "T1", capability = "distribution.stores",
            description = "Implement one repository method — CtStoreRepository.findAll() in the project "
                    + "module's infrastructure layer — returning the raw SDK List<Store>. StoreService maps "
                    + "it so GET /api/stores lists the project's Stores — with each store's "
                    + "distribution-channel keys — for the storefront's store bar and region switcher.",
            hint = "Docs: Stores — a Store scopes catalog, channels and settings to a market "
                    + "(docs.commercetools.com/api/projects/stores).")
    @GetMapping("/api/stores")
    public List<StoreView> listStores() {
        return storeService.listStores().stream().map(StoreViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "project", session = "Session 1", taskNumber = 3,
            title = "Get store by key", tier = "T1", capability = "distribution.storeByKey",
            description = "Implement one repository method — CtStoreRepository.findByKey(key) in the "
                    + "project module's infrastructure layer — returning the raw SDK Store for GET "
                    + "/api/stores/{key}. A commercetools 404 surfaces as a NotFoundException → HTTP 404 "
                    + "via the shared error advice, so an unknown key returns a clean 404.",
            hint = "Docs: Get Store by Key — prefer key over id for lookups "
                    + "(docs.commercetools.com/api/projects/stores#get-store-by-key).")
    @GetMapping("/api/stores/{key}")
    public StoreView getStore(@PathVariable String key) {
        return StoreViewMapper.toView(storeService.getStore(key));
    }

    @TaskDescription(
            module = "project", session = "Session 1", taskNumber = 4,
            title = "Active store / region resolution", tier = "T2", capability = "distribution.store",
            description = "Implement the T2 logic in StoreService.storeContext(store) in the project "
                    + "module's application layer — no new SDK call; reuse the repository read "
                    + "StoreRepository.findAll() from 1.2. Resolve the active store from the ?store= param "
                    + "with a safe fallback, and build the domain StoreContext (active store + switch list "
                    + "+ its languages/countries/channel keys) so GET /api/store-context powers LHC's "
                    + "US/UK/DE region switcher without ever crashing on a bad value.",
            hint = "Docs: Stores — languages, countries and distributionChannels on a Store "
                    + "(docs.commercetools.com/api/projects/stores).",
            decisions = {
                    "What is a \"region\"? — the reference maps region = Store (not Channel, not project "
                            + "scope); be ready to defend it.",
                    "How the active store is chosen — from the ?store=<key> param (later: cookie / geo / "
                            + "header).",
                    "The fallback policy — absent/empty/unknown ?store= → first store; no stores → an "
                            + "empty-but-valid context; never throw. This is the heart of the task.",
                    "The shape of StoreContext the storefront needs back — active store + switch list + "
                            + "region facets (languages/countries/channel keys)."
            })
    @GetMapping("/api/store-context")
    public StoreContextView storeContext(@RequestParam(required = false) String store) {
        return StoreViewMapper.toView(storeService.storeContext(store));
    }
}
