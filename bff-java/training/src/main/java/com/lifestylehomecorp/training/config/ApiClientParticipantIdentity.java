package com.lifestylehomecorp.training.config;

import com.commercetools.api.client.ProjectApiRoot;
import com.lifestylehomecorp.platform.config.CtpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves the participant from the **API client** the BFF authenticates with — no manual
 * PARTICIPANT_ID, so identity can't drift.
 *
 * <p>Reads the API client's NAME (e.g. "vishal_shah_api") via {@code GET /api-clients/{clientId}} and
 * uses it as the progress key. That endpoint needs the {@code manage_api_clients} scope; if the
 * client lacks it (or the lookup fails), we fall back to the {@code clientId} (always known from
 * config) so tracking still works — just with a less friendly key. Resolved once, lazily, cached.
 */
@Component
public class ApiClientParticipantIdentity implements ParticipantIdentity {

    private static final Logger log = LoggerFactory.getLogger(ApiClientParticipantIdentity.class);

    private final ProjectApiRoot apiRoot;
    private final CtpProperties ctp;
    private final TrainingProperties props;

    private volatile boolean resolved;
    private volatile String id;
    private volatile String name;

    public ApiClientParticipantIdentity(ProjectApiRoot apiRoot, CtpProperties ctp, TrainingProperties props) {
        this.apiRoot = apiRoot;
        this.ctp = ctp;
        this.props = props;
    }

    @Override
    public String id() {
        ensureResolved();
        return id;
    }

    @Override
    public String name() {
        ensureResolved();
        return name;
    }

    private synchronized void ensureResolved() {
        if (resolved) {
            return;
        }
        String clientId = ctp.getClientId();
        try {
            String apiClientName = apiRoot.apiClients()
                    .withId(clientId)
                    .get()
                    .executeBlocking()
                    .getBody()
                    .getName();
            if (apiClientName != null && !apiClientName.isBlank()) {
                this.name = apiClientName;
                this.id = sanitizeKey(apiClientName);
                log.info("Participant identity resolved from API client name '{}' (key '{}').", apiClientName, id);
                resolved = true;
                return;
            }
        } catch (Exception e) {
            log.warn("Could not read the API client name (grant 'manage_api_clients' for readable ids) — "
                    + "falling back to clientId. Cause: {}", e.getMessage());
        }
        this.id = clientId;
        this.name = (props.getParticipantName() != null && !props.getParticipantName().isBlank())
                ? props.getParticipantName()
                : clientId;
        resolved = true;
    }

    /** commercetools Custom Object keys allow [-_~.a-zA-Z0-9], length 2..256. */
    private static String sanitizeKey(String raw) {
        String key = raw.trim().replaceAll("[^-_~.a-zA-Z0-9]", "_");
        if (key.length() < 2) {
            key = "participant_" + key;
        }
        return key.length() > 256 ? key.substring(0, 256) : key;
    }
}
