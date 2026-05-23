package com.dani.bff.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

/**
 * Converts uncaught application exceptions into structured API errors.
 */
@RestControllerAdvice
public class GlobalErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    /**
     * Handles downstream failures that escape resilience fallbacks.
     *
     * @param exception downstream exception
     * @param exchange current request exchange
     * @return structured error response
     */
    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiError> handleDownstreamFailure(
            DownstreamServiceException exception,
            ServerWebExchange exchange) {
        log.warn("Downstream service failed: service={} status={}", exception.getServiceName(), exception.getStatusCode());
        return error(HttpStatus.BAD_GATEWAY, "A downstream service could not complete the request", exchange);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return error(status, exception.getReason() != null ? exception.getReason() : status.getReasonPhrase(), exchange);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleUnexpected(Throwable exception, ServerWebExchange exchange) {
        log.error("Unexpected BFF failure", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "The BFF could not complete the request", exchange);
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        return ResponseEntity.status(status).body(ApiError.of(status, message, path));
    }
}
