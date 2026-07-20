package com.lifestylehomecorp.training.progress;

import com.lifestylehomecorp.training.catalog.TaskCatalog;
import com.lifestylehomecorp.training.config.ParticipantIdentity;
import com.lifestylehomecorp.training.domain.ParticipantProgress;
import com.lifestylehomecorp.training.domain.TaskDefinition;
import com.lifestylehomecorp.training.domain.TaskEntry;
import com.lifestylehomecorp.training.domain.TaskProgress;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the CURRENT participant's progress in memory and writes through to the {@link ProgressStore}.
 *
 * <p>Completion never blocks the API response: the in-memory cache is updated synchronously and the
 * store write is fire-and-forget with error logging, so a tracking failure never fails the real
 * request. The master task list is derived from {@link TaskCatalog}; only progress is persisted.
 */
@Service
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final ProgressStore store;
    private final TaskCatalog catalog;
    private final ParticipantIdentity identity;

    private final ConcurrentHashMap<String, TaskProgress> current = new ConcurrentHashMap<>();

    public ProgressService(ProgressStore store, TaskCatalog catalog, ParticipantIdentity identity) {
        this.store = store;
        this.catalog = catalog;
        this.identity = identity;
    }

    @PostConstruct
    void loadOnStartup() {
        try {
            store.load(identity.id())
                    .ifPresent(pp -> pp.tasks().forEach(
                            te -> current.put(te.id(), new TaskProgress(te.completed(), te.completedAt()))));
        } catch (Exception e) {
            log.warn("Could not seed progress on startup: {}", e.getMessage());
        }
    }

    /** Mark a task complete (idempotent). Updates memory synchronously, persists fire-and-forget. */
    public void markComplete(String taskId) {
        TaskProgress existing = current.get(taskId);
        if (existing != null && existing.completed()) {
            return;
        }
        current.put(taskId, new TaskProgress(true, Instant.now().toString()));
        persistAsync();
    }

    public boolean isCompleted(String taskId) {
        TaskProgress tp = current.get(taskId);
        return tp != null && tp.completed();
    }

    /** Current participant's progress snapshot. */
    public ParticipantProgress currentProgress() {
        return snapshot();
    }

    /** Distinct capabilities of the current participant's completed tasks (drives FeatureGate). */
    public List<String> unlockedCapabilities() {
        return catalog.tasks().stream()
                .filter(t -> isCompleted(t.id()))
                .map(TaskDefinition::capability)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .toList();
    }

    public List<ParticipantProgress> allProgress() {
        return store.loadAll();
    }

    /** Reset the current participant's progress. */
    public void reset() {
        current.clear();
        persistAsync();
    }

    private ParticipantProgress snapshot() {
        List<TaskEntry> entries = current.entrySet().stream()
                .map(e -> new TaskEntry(e.getKey(), e.getValue().completed(), e.getValue().completedAt()))
                .toList();
        return new ParticipantProgress(
                identity.id(),
                identity.name(),
                Instant.now().toString(),
                entries);
    }

    private void persistAsync() {
        ParticipantProgress pp = snapshot();
        CompletableFuture.runAsync(() -> {
            try {
                store.save(pp);
            } catch (Exception e) {
                log.warn("Progress write failed for {} (ignored): {}", pp.participantId(), e.getMessage());
            }
        });
    }

    Map<String, TaskProgress> currentTasks() {
        return Map.copyOf(current);
    }
}
