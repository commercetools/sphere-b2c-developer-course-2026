package com.lifestylehomecorp.training.web;

import com.lifestylehomecorp.platform.annotations.TaskDescription;
import com.lifestylehomecorp.platform.observability.ApproachContext;
import com.lifestylehomecorp.training.domain.TaskDefinition;
import com.lifestylehomecorp.training.telemetry.ApproachEvaluator;
import com.lifestylehomecorp.training.telemetry.ApproachExpectation;
import com.lifestylehomecorp.training.telemetry.ApproachRules;
import com.lifestylehomecorp.training.telemetry.ApproachTelemetryService;
import com.lifestylehomecorp.training.telemetry.TaskSignal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Trainer-only approach telemetry (see {@code trainer-approach-telemetry.md}). Opens an
 * {@link ApproachContext} scope before a {@code @TaskDescription} handler runs so the commercetools
 * client middleware can attribute each API call to the task; after a 2xx completion it evaluates the
 * observed calls against the task's expected approach and records a {@link TaskSignal}.
 *
 * <p>Enabled by default; disable with {@code training.approach-telemetry=off}. Passive throughout:
 * it never alters the response and always closes the scope, even on error.
 */
@Component
@ConditionalOnProperty(name = "training.approach-telemetry", havingValue = "on", matchIfMissing = true)
public class ApproachTelemetryInterceptor implements HandlerInterceptor {

    private final ApproachRules rules;
    private final ApproachEvaluator evaluator;
    private final ApproachTelemetryService telemetry;

    public ApproachTelemetryInterceptor(ApproachRules rules, ApproachEvaluator evaluator,
                                        ApproachTelemetryService telemetry) {
        this.rules = rules;
        this.evaluator = evaluator;
        this.telemetry = telemetry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String taskId = taskIdOf(handler);
        // Only open a scope for tasks that have an expected-approach rule — no rule, no overhead.
        if (taskId != null && rules.forTask(taskId) != null) {
            ApproachContext.begin(taskId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        ApproachContext.Scope scope = ApproachContext.end();
        try {
            if (scope == null || ex != null) {
                return; // no scope, or the handler failed — nothing to evaluate
            }
            int status = response.getStatus();
            if (status < 200 || status >= 300) {
                return; // only evaluate completed (2xx) attempts, matching the completion rule
            }
            ApproachExpectation exp = rules.forTask(scope.taskId());
            if (exp == null) {
                return;
            }
            telemetry.record(evaluator.evaluate(exp, scope.calls()));
        } catch (Exception ignored) {
            // Never let telemetry affect the request.
        }
    }

    private static String taskIdOf(Object handler) {
        if (!(handler instanceof HandlerMethod hm)) {
            return null;
        }
        TaskDescription ann = hm.getMethodAnnotation(TaskDescription.class);
        return ann == null ? null : TaskDefinition.idOf(ann.module(), ann.session(), ann.taskNumber());
    }
}
