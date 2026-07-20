package com.lifestylehomecorp.training.progress;

import com.lifestylehomecorp.training.domain.ParticipantProgress;

import java.util.List;
import java.util.Optional;

/**
 * Persistence port for per-participant progress. Pluggable: a commercetools Custom Object store
 * or a local in-memory/JSON store can be swapped without touching callers.
 */
public interface ProgressStore {

    /** Load one participant's progress, or empty if none stored yet. */
    Optional<ParticipantProgress> load(String participantId);

    /** Version-aware upsert of one participant's progress. */
    void save(ParticipantProgress progress);

    /** Load every participant's progress (trainer dashboard). */
    List<ParticipantProgress> loadAll();
}
