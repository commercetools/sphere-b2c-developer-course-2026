package com.lifestylehomecorp.project.application;

import com.lifestylehomecorp.platform.errors.TaskNotImplementedException;
import com.lifestylehomecorp.project.domain.StoreContext;
import com.lifestylehomecorp.project.domain.StoreSummary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application logic for Stores. The repository returns raw SDK types; this service maps them to the
 * domain and holds all non-SDK logic.
 *
 * <ul>
 *   <li><b>T1 (1.2, 1.3):</b> {@code listStores}/{@code getStore} map SDK → domain here — trainer
 *       provided, <b>not</b> edited by participants. Participants implement only the SDK call in
 *       {@link StoreRepository}'s implementation.</li>
 *   <li><b>T2 (1.4):</b> {@code storeContext} is the human-in-the-loop task — the participant writes
 *       the resolution/fallback logic here, composing the SDK reads exposed by the repository.</li>
 * </ul>
 */
@Service
public class StoreService {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /** Task 1.2 — map the SDK stores to domain summaries. */
    public List<StoreSummary> listStores() {
        return storeRepository.findAll().stream().map(StoreMapper::toSummary).toList();
    }

    /** Task 1.3 — map one SDK store to a domain summary. */
    public StoreSummary getStore(String key) {
        return StoreMapper.toSummary(storeRepository.findByKey(key));
    }

    /**
     * Task 1.4 (T2) — resolve the active store/region for the storefront.
     * Participant TODO: pick the active store (from {@code storeKey} or a default), decide the
     * fallback when it is missing/unknown (never throw), and build a {@link StoreContext} from the
     * repository's SDK reads. This is the design task — the SDK reads already exist in the repository.
     */
    public StoreContext storeContext(String storeKey) {
        // TODO (Task 1.4): resolve the active store/region — pick the active store (from storeKey or a
        // default), decide the fallback when it is missing/unknown (never throw), and build a
        // StoreContext from the repository's SDK reads (findAll, Task 1.2). This is the design task.
        // See the @TaskDescription hint + session-tasks-detailed.md.
        throw new TaskNotImplementedException("1.4");
    }
}
