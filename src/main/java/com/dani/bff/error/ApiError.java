package com.dani.bff.error;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Structured JSON error returned by the BFF for predictable client handling.")
public record ApiError(
        @Schema(description = "Time the error response was produced.", example = "2026-05-23T11:24:56.339Z")
        Instant timestamp,
        @Schema(description = "HTTP status code.", example = "401")
        int status,
        @Schema(description = "HTTP status reason phrase.", example = "Unauthorized")
        String error,
        @Schema(description = "Stable, human-readable summary.", example = "Authentication is required")
        String message,
        @Schema(description = "Request path that failed.", example = "/api/dashboard")
        String path) {

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
