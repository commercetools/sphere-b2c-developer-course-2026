package com.lifestylehomecorp.training.config;

/**
 * Who the participant is, derived from the API client the BFF authenticates with (see
 * {@link ApiClientParticipantIdentity}). An interface so it can be injected/mocked cleanly.
 */
public interface ParticipantIdentity {

    /** Stable Custom Object key for this participant. */
    String id();

    /** Human-friendly display name for the trainer dashboard. */
    String name();
}
