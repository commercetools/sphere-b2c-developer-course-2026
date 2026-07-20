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
            description = "List the project's Stores so GET /api/stores returns them for the store bar and region switcher.",
            hint = "SDK: apiRoot.stores().get()...getBody().getResults(); expand distributionChannels[*] to surface channel keys | Docs: Stores (docs.commercetools.com/api/projects/stores)")
    @GetMapping("/api/stores")
    public List<StoreView> listStores() {
        return storeService.listStores().stream().map(StoreViewMapper::toView).toList();
    }

    @TaskDescription(
            module = "project", session = "Session 1", taskNumber = 3,
            title = "Get store by key", tier = "T1", capability = "distribution.storeByKey",
            description = "Fetch one Store by its key for GET /api/stores/{key}, with a clean 404 for an unknown key.",
            hint = "SDK: apiRoot.stores().withKey(key).get()...getBody() | a commercetools 404 -> NotFoundException -> HTTP 404 via the error advice | Docs: Stores > Get Store by Key (docs.commercetools.com/api/projects/stores#get-store-by-key)")
    @GetMapping("/api/stores/{key}")
    public StoreView getStore(@PathVariable String key) {
        return StoreViewMapper.toView(storeService.getStore(key));
    }

    @TaskDescription(
            module = "project", session = "Session 1", taskNumber = 4,
            title = "Active store / region resolution", tier = "T2", capability = "distribution.store",
            description = "LHC runs US/UK/DE storefronts and wants a region switcher: resolve the active store from ?store= with a safe fallback that never crashes.",
            hint = "T2 logic in StoreService.storeContext -- no new SDK call; compose findAll() from 1.2. Decide region=Store vs Channel vs project; resolve active from ?store=; fall back to the first store, never throw | Docs: Stores (docs.commercetools.com/api/projects/stores)")
    @GetMapping("/api/store-context")
    public StoreContextView storeContext(@RequestParam(required = false) String store) {
        return StoreViewMapper.toView(storeService.storeContext(store));
    }
}
