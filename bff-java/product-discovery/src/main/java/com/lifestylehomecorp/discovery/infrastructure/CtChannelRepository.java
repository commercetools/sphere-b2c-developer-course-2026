package com.lifestylehomecorp.discovery.infrastructure;

import com.commercetools.api.client.ProjectApiRoot;
import com.lifestylehomecorp.discovery.application.ChannelRepository;
import org.springframework.stereotype.Repository;

import java.util.concurrent.ConcurrentHashMap;

/**
 * SDK-backed channel key→id resolver with a process-lifetime cache. Channels are few and stable, so
 * caching keeps channel-scoped price selection off the hot path (one Channels read per distinct key,
 * then served from cache — so a warm PLP is a single Product Search call). A missing channel resolves
 * to {@code null} rather than throwing, so a stale key can't fail a search.
 *
 * <p>Explicit bean name: the aggregator scans every module, and {@code catalog} carries an
 * identically-named {@code CtChannelRepository}; a distinct bean name avoids the duplicate-name clash
 * (the two implement different module-local {@code ChannelRepository} interfaces, so injection stays
 * unambiguous by type).
 */
@Repository("discoveryChannelRepository")
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
            return null;
        }
    }
}
