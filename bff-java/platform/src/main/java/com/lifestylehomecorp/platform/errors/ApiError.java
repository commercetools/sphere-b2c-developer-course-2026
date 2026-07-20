package com.lifestylehomecorp.platform.errors;

/**
 * Uniform error body returned by the BFF. Deliberately small — it never leaks
 * commercetools SDK exception types to the client.
 *
 * @param status  HTTP status code
 * @param message human-readable summary
 * @param taskId  the course task id when the error is a not-yet-implemented task, otherwise null
 */
public record ApiError(int status, String message, String taskId) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, null);
    }
}
