package com.lifestylehomecorp.catalog.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.lifestylehomecorp.catalog.application.ChannelRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SDK-backed channel key→id resolver with a process-lifetime cache. Channels are few and stable, so
 * caching the lookup keeps channel-scoped price selection off the hot path (one Channels read per
 * distinct key). A missing channel resolves to {@code null} rather than throwing, so a stale key
 * can't fail a PLP/PDP — price selection simply omits the channel and falls back.
 */
@Repository
public class CtChannelRepository implements ChannelRepository {

    private final ProjectApiRoot apiRoot;
    private final ConcurrentHashMap<String, String> idByKey = new ConcurrentHashMap<>();

    public CtChannelRepository(ProjectApiRoot apiRoot) {
        this.apiRoot = apiRoot;
    }

    @Override
    public String idByKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return idByKey.computeIfAbsent(key, this::lookup);
    }

    private String lookup(String key) {
        try {
            return apiRoot.channels().withKey(key).get().executeBlocking().getBody().getId();
        } catch (Exception e) {
            // Unknown/unreachable channel → skip channel-scoped selection (never break the read).
            return null;
        }
    }
}
