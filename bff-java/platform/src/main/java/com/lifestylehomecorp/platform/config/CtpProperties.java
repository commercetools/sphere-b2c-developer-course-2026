package com.lifestylehomecorp.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code ctp.*} properties (sourced from the gitignored .env via spring-dotenv)
 * to typed fields. Never hardcode credentials — they flow in from the environment only.
 */
@ConfigurationProperties(prefix = "ctp")
public class CtpProperties {

    /** commercetools Project key. */
    private String projectKey;
    /** API Client id. */
    private String clientId;
    /** API Client secret. */
    private String clientSecret;
    /** OAuth host, e.g. https://auth.europe-west1.gcp.commercetools.com */
    private String authUrl;
    /** API host, e.g. https://api.europe-west1.gcp.commercetools.com */
    private String apiUrl;
    /** Optional in-store key used by store-scoped (in-store) endpoints. */
    private String storeKey;

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getStoreKey() {
        return storeKey;
    }

    public void setStoreKey(String storeKey) {
        this.storeKey = storeKey;
    }
}
