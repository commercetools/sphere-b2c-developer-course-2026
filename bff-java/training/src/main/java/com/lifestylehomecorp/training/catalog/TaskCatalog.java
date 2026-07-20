package com.lifestylehomecorp.training.catalog;

import com.lifestylehomecorp.platform.annotations.TaskDescription;
import com.lifestylehomecorp.training.domain.TaskDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the master task list by scanning every controller handler for {@code @TaskDescription}
 * and deriving the endpoint + HTTP method from its Spring request mapping. Uses Spring's
 * {@link RequestMappingHandlerMapping} (not the Reflections library).
 *
 * <p>Built lazily on first access so all handlers are registered by the time we scan. The list is
 * derived, never persisted — adding/editing a task means only annotating a controller method.
 */
@Component
public class TaskCatalog {

    // ObjectProvider defers resolution until first scan — avoids a construction-time cycle with
    // the MVC config (which itself consumes our WebMvcConfigurer/interceptor).
    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;
    private volatile List<TaskDefinition> tasks;

    public TaskCatalog(ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        this.handlerMappingProvider = handlerMappingProvider;
    }

    public List<TaskDefinition> tasks() {
        if (tasks == null) {
            synchronized (this) {
                if (tasks == null) {
                    tasks = scan();
                }
            }
        }
        return tasks;
    }

    private List<TaskDefinition> scan() {
        return handlerMappingProvider.getObject().getHandlerMethods().entrySet().stream()
                .map(e -> toDefinition(e.getKey(), e.getValue()))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(TaskDefinition::module)
                        .thenComparing(TaskDefinition::session)
                        .thenComparingInt(TaskDefinition::taskNumber))
                .toList();
    }

    private TaskDefinition toDefinition(RequestMappingInfo info, HandlerMethod handler) {
        TaskDescription ann = handler.getMethodAnnotation(TaskDescription.class);
        if (ann == null) {
            return null;
        }
        return new TaskDefinition(
                TaskDefinition.idOf(ann.module(), ann.session(), ann.taskNumber()),
                ann.module(),
                ann.session(),
                ann.taskNumber(),
                ann.title(),
                ann.tier(),
                ann.capability(),
                endpointOf(info),
                httpMethodOf(info),
                ann.description(),
                ann.hint());
    }

    private static String endpointOf(RequestMappingInfo info) {
        PathPatternsRequestCondition patterns = info.getPathPatternsCondition();
        if (patterns != null && !patterns.getPatterns().isEmpty()) {
            return patterns.getPatterns().iterator().next().getPatternString();
        }
        Set<String> direct = info.getDirectPaths();
        return direct.isEmpty() ? "" : direct.iterator().next();
    }

    private static String httpMethodOf(RequestMappingInfo info) {
        return info.getMethodsCondition().getMethods().stream()
                .findFirst()
                .map(Enum::name)
                .orElse("GET");
    }

    /** taskId -> definition, for quick lookups when reporting progress. */
    public Map<String, TaskDefinition> byId() {
        return tasks().stream().collect(
                java.util.stream.Collectors.toMap(TaskDefinition::id, t -> t, (a, b) -> a,
                        java.util.LinkedHashMap::new));
    }
}
