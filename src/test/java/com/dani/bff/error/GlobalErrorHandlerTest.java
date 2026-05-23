package com.dani.bff.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

class GlobalErrorHandlerTest {

    private final GlobalErrorHandler handler = new GlobalErrorHandler();

    @Test
    void downstreamFailureReturnsBadGatewayError() {
        MockServerWebExchange exchange = exchange("/api/dashboard");
        DownstreamServiceException exception = new DownstreamServiceException(
                "user-service",
                HttpStatus.SERVICE_UNAVAILABLE,
                "maintenance");

        var response = handler.handleDownstreamFailure(exception, exchange);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("A downstream service could not complete the request");
        assertThat(response.getBody().path()).isEqualTo("/api/dashboard");
    }

    @Test
    void responseStatusExceptionKeepsStatusAndReason() {
        MockServerWebExchange exchange = exchange("/missing");

        var response = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing"), exchange);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Missing");
    }

    @Test
    void unexpectedExceptionReturnsInternalServerError() {
        MockServerWebExchange exchange = exchange("/api/dashboard");

        var response = handler.handleUnexpected(new IllegalStateException("boom"), exchange);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("The BFF could not complete the request");
    }

    private static MockServerWebExchange exchange(String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
    }
}
