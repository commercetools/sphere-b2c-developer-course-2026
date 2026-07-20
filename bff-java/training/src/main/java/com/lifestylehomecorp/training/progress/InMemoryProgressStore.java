package com.lifestylehomecorp.training.progress;

import com.lifestylehomecorp.training.domain.ParticipantProgress;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local, non-persistent progress store — the pluggable alternative to the commercetools store
 * (used for local dev and tests). Selected with {@code training.progress-store=memory}.
 */
@Component
@ConditionalOnProperty(name = "training.progress-store", havingValue = "memory")
public class InMemoryProgressStore implements ProgressStore {

    private final ConcurrentHashMap<String, ParticipantProgress> store = new ConcurrentHashMap<>();

    @Override
    public Optional<ParticipantProgress> load(String participantId) {
        return Optional.ofNullable(store.get(participantId));
    }

    @Override
    public void save(ParticipantProgress progress) {
        store.put(progress.participantId(), progress);
    }

    @Override
    public List<ParticipantProgress> loadAll() {
        return List.copyOf(store.values());
    }
}
