package com.dani.bff.error;

import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * Structured error body returned by the BFF for predictable client-side handling.
 *
 * @param timestamp time the error response was produced
 * @param status numeric HTTP status
 * @param error HTTP status reason phrase
 * @param message stable, human-readable summary
 * @param path request path that failed
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {

    /**
     * Creates an API error with the current timestamp.
     *
     * @param status HTTP status to represent
     * @param message stable message for clients and logs
     * @param path request path
     * @return structured API error
     */
    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path);
    }
}
