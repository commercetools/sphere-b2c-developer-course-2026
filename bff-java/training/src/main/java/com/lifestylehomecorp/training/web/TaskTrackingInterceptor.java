package com.lifestylehomecorp.training.web;

import com.lifestylehomecorp.platform.annotations.TaskDescription;
import com.lifestylehomecorp.training.domain.TaskDefinition;
import com.lifestylehomecorp.training.progress.ProgressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Marks a task complete when its annotated endpoint returns 2xx with no exception. A stubbed
 * infrastructure method throws and returns 501 (incomplete); an implemented one returns 2xx
 * (complete). Tracking never affects the real response.
 */
@Component
public class TaskTrackingInterceptor implements HandlerInterceptor {

    private final ProgressService progressService;

    public TaskTrackingInterceptor(ProgressService progressService) {
        this.progressService = progressService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        if (ex != null || !(handler instanceof HandlerMethod handlerMethod)) {
            return;
        }
        TaskDescription ann = handlerMethod.getMethodAnnotation(TaskDescription.class);
        if (ann == null) {
            return;
        }
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return;
        }
        try {
            progressService.markComplete(
                    TaskDefinition.idOf(ann.module(), ann.session(), ann.taskNumber()));
        } catch (Exception ignored) {
            // Never let progress tracking affect the request.
        }
    }
}
