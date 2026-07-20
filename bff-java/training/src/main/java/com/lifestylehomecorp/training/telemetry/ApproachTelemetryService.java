package com.lifestylehomecorp.training.telemetry;

import com.lifestylehomecorp.training.config.ParticipantIdentity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the CURRENT participant's approach signals in memory and writes through to the
 * {@link ApproachTelemetryStore}. Recording never blocks or fails the real API response: the
 * in-memory map is updated synchronously and the store write is fire-and-forget with error logging
 * (mirrors {@code ProgressService}). Trainer-only — nothing here affects the participant's app.
 */
@Service
public class ApproachTelemetryService {

    private static final Logger log = LoggerFactory.getLogger(ApproachTelemetryService.class);

    private final ApproachTelemetryStore store;
    private final ParticipantIdentity identity;

    /** taskId -> latest signal for the current participant. */
    private final ConcurrentHashMap<String, TaskSignal> current = new ConcurrentHashMap<>();

    public ApproachTelemetryService(ApproachTelemetryStore store, ParticipantIdentity identity) {
        this.store = store;
        this.identity = identity;
    }

    @PostConstruct
    void loadOnStartup() {
        try {
            store.load(identity.id())
                    .ifPresent(pt -> pt.signals().forEach(s -> current.put(s.taskId(), s)));
        } catch (Exception e) {
            log.warn("Could not seed telemetry on startup: {}", e.getMessage());
        }
    }

    /** Record the latest signal for a task (overwrites any earlier attempt). Persists fire-and-forget. */
    public void record(TaskSignal signal) {
        current.put(signal.taskId(), signal);
        persistAsync();
    }

    /** The current participant's telemetry snapshot. */
    public ParticipantTelemetry currentTelemetry() {
        return snapshot();
    }

    /** Every participant's telemetry (trainer dashboard). */
    public List<ParticipantTelemetry> allTelemetry() {
        return store.loadAll();
    }

    private ParticipantTelemetry snapshot() {
        return new ParticipantTelemetry(
                identity.id(), identity.name(), Instant.now().toString(),
                List.copyOf(current.values()));
    }

    private void persistAsync() {
        ParticipantTelemetry pt = snapshot();
        CompletableFuture.runAsync(() -> {
            try {
                store.save(pt);
            } catch (Exception e) {
                log.warn("Telemetry write failed for {} (ignored): {}", pt.participantId(), e.getMessage());
            }
        });
    }
}
