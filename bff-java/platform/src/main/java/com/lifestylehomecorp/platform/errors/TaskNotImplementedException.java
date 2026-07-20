package com.lifestylehomecorp.platform.errors;

/**
 * Marks a course task that has not been implemented yet. Thrown from a stubbed
 * infrastructure adapter method; {@link CtErrorAdvice} maps it to HTTP 501 so the
 * training companion can render a clear "pending task" state.
 *
 * <p>Completing a task means replacing the {@code throw new TaskNotImplementedException(...)}
 * in exactly one infrastructure method with the real commercetools SDK call.
 */
public class TaskNotImplementedException extends RuntimeException {

    private final String taskId;

    public TaskNotImplementedException(String taskId) {
        super("Task " + taskId + " is not implemented yet.");
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
