package com.lifestylehomecorp.platform.errors;

import com.commercetools.api.models.error.ErrorResponse;
import io.vrap.rmf.base.client.ApiHttpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cross-cutting error mapping for the whole BFF. Translates commercetools SDK
 * exceptions and the course's {@link TaskNotImplementedException} into clean HTTP
 * responses, so no SDK exception type ever reaches an HTTP client.
 *
 * <p>This is the only web-aware type in {@code platform}; it defines no endpoints.
 */
@RestControllerAdvice
public class CtErrorAdvice {

    /** Not-yet-implemented course task -> 501 Not Implemented. */
    @ExceptionHandler(TaskNotImplementedException.class)
    public ResponseEntity<ApiError> handleNotImplemented(TaskNotImplementedException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_IMPLEMENTED)
                .body(new ApiError(HttpStatus.NOT_IMPLEMENTED.value(), ex.getMessage(), ex.getTaskId()));
    }

    /**
     * Any commercetools API error. Every SDK HTTP exception extends
     * {@link ApiHttpException} and carries the upstream status code, which we pass through
     * (falling back to 502 if the status is not a usable client/server code).
     */
    @ExceptionHandler(ApiHttpException.class)
    public ResponseEntity<ApiError> handleCommercetools(ApiHttpException ex) {
        int upstream = ex.getStatusCode();
        HttpStatus status = HttpStatus.resolve(upstream);
        if (status == null || upstream < 400) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return ResponseEntity.status(status).body(ApiError.of(status.value(), commercetoolsMessage(ex, status)));
    }

    /**
     * The clean, client-safe message for a commercetools API error — the {@code message} from the
     * upstream {@link ErrorResponse} body (e.g. "The Resource with key 'x' was not found."), NOT
     * {@link ApiHttpException#getMessage()}, which is the SDK's verbose diagnostic dump (full URL,
     * correlation id, request/response headers, SDK/JVM versions). Falls back to the status reason
     * phrase when the body isn't a parseable commercetools error.
     */
    private static String commercetoolsMessage(ApiHttpException ex, HttpStatus status) {
        try {
            ErrorResponse body = ex.getBodyAs(ErrorResponse.class);
            if (body != null && body.getMessage() != null && !body.getMessage().isBlank()) {
                return body.getMessage();
            }
        } catch (Exception ignored) {
            // Body absent or not a commercetools ErrorResponse — fall back to a generic status message.
        }
        return status.getReasonPhrase();
    }

    /**
     * A deliberate HTTP status raised by our own code (e.g. a slug that matched no product ->
     * 404). Pass the chosen status through. More specific than the catch-all below, so it wins.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CtErrorAdvice.class);

    /** Anything else -> 500, without leaking the exception type or stack trace. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error."));
    }
}
