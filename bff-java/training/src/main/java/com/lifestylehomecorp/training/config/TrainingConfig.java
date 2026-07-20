package com.lifestylehomecorp.training.config;

import com.lifestylehomecorp.training.web.ApproachTelemetryInterceptor;
import com.lifestylehomecorp.training.web.TaskTrackingInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Enables {@link TrainingProperties} and registers the task-tracking interceptor across all
 * controller endpoints (so any annotated domain handler is tracked when assembled by the aggregator).
 * Also registers the trainer-only approach-telemetry interceptor when it is enabled
 * ({@code training.approach-telemetry=on}, the default) — injected via {@link ObjectProvider} so the
 * config still starts when it is switched off.
 */
@Configuration
@EnableConfigurationProperties(TrainingProperties.class)
public class TrainingConfig implements WebMvcConfigurer {

    private final TaskTrackingInterceptor taskTrackingInterceptor;
    private final ObjectProvider<ApproachTelemetryInterceptor> approachTelemetryInterceptor;

    public TrainingConfig(TaskTrackingInterceptor taskTrackingInterceptor,
                          ObjectProvider<ApproachTelemetryInterceptor> approachTelemetryInterceptor) {
        this.taskTrackingInterceptor = taskTrackingInterceptor;
        this.approachTelemetryInterceptor = approachTelemetryInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(taskTrackingInterceptor);
        approachTelemetryInterceptor.ifAvailable(registry::addInterceptor);
    }
}
