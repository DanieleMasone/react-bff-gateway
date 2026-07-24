package com.dani.bff.error;

import org.springframework.http.HttpStatusCode;

/**
 * Signals that a downstream HTTP service did not provide a usable response.
 */
public class DownstreamServiceException extends RuntimeException {

    private final String serviceName;
    private final int statusCode;

    /**
     * Creates an exception with downstream service context.
     *
     * @param serviceName logical downstream service name
     * @param statusCode HTTP status returned by the downstream service
     * @param responseBody summarized response body for diagnostics
     */
    public DownstreamServiceException(String serviceName, HttpStatusCode statusCode, String responseBody) {
        super(serviceName + " returned HTTP " + statusCode.value() + ": " + responseBody);
        this.serviceName = serviceName;
        this.statusCode = statusCode.value();
    }

    public String getServiceName() {
        return serviceName;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
