package com.lifestylehomecorp.platform.ctclient;

import com.commercetools.api.client.ProjectApiRoot;
import com.commercetools.api.defaultconfig.ApiRootBuilder;
import com.commercetools.http.okhttp3.CtOkHttp3Client;
import com.lifestylehomecorp.platform.config.CtpProperties;
import com.lifestylehomecorp.platform.observability.ApproachContext;
import io.vrap.rmf.base.client.oauth2.ClientCredentials;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

/**
 * Creates the single {@link ProjectApiRoot} bean for the whole application.
 *
 * <p>The ProjectApiRoot is a thread-safe connection pool — create it ONCE and inject it
 * everywhere (only the infrastructure layer of each domain module should hold it).
 * We build it from the OkHttp3 client so the SDK's HTTP stack does not clash with any
 * OkHttp brought in elsewhere on the Spring Boot classpath.
 */
@Configuration
@EnableConfigurationProperties(CtpProperties.class)
public class CtApiRootConfig {

    @Bean(destroyMethod = "close")
    public ProjectApiRoot projectApiRoot(CtpProperties props) {
        String apiHost = hostOf(props.getApiUrl());
        return ApiRootBuilder.of(new CtOkHttp3Client())
                .defaultClient(
                        ClientCredentials.of()
                                .withClientId(props.getClientId())
                                .withClientSecret(props.getClientSecret())
                                .build(),
                        oauthTokenUrl(props.getAuthUrl()),
                        props.getApiUrl())
                // Trainer-only approach telemetry: passively record each outbound API call while a
                // task endpoint is in flight (see ApproachContext + trainer-approach-telemetry.md).
                // Skips the OAuth host and is a no-op when no task scope is open, so it costs nothing
                // on normal traffic and can never affect the real request.
                .addMiddleware((request, next) -> {
                    try {
                        URI uri = request.getUri();
                        if (uri != null && apiHost != null && apiHost.equals(uri.getHost())) {
                            ApproachContext.record(
                                    request.getMethod() != null ? request.getMethod().name() : "", uri);
                        }
                    } catch (RuntimeException ignored) {
                        // never let telemetry break the call
                    }
                    return next.apply(request);
                })
                .build(props.getProjectKey());
    }

    private static String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * {@code defaultClient} expects the full OAuth token endpoint (e.g. .../oauth/token),
     * while CTP_AUTH_URL in .env is the auth host. Append the path when it is not already
     * present so either form works.
     */
    private static String oauthTokenUrl(String authUrl) {
        String base = authUrl.replaceAll("/+$", "");
        return base.endsWith("/oauth/token") ? base : base + "/oauth/token";
    }
}
