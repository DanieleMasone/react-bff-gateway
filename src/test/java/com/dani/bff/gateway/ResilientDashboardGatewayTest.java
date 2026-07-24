package com.dani.bff.gateway;

import com.dani.bff.config.DownstreamProperties;
import com.dani.bff.dto.UserProfile;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ResilientDashboardGatewayTest {

    private final DownstreamProperties properties = new DownstreamProperties();
    private final WebClient webClient = WebClient.create("http://localhost");
    private final ResilientDashboardGateway gateway = new ResilientDashboardGateway(
            new UserServiceClient(webClient, properties),
            new ProductServiceClient(webClient, properties));

    @Test
    void expectedDownstreamFailureUsesStableFallback() {
        StepVerifier.create(gateway.fallbackUserProfile("user-123", new TimeoutException("timed out")))
                .expectNext(UserProfile.unavailable("user-123"))
                .verifyComplete();
    }

    @Test
    void unexpectedProgrammingFailureIsPropagated() {
        IllegalStateException failure = new IllegalStateException("unexpected state");

        StepVerifier.create(gateway.fallbackUserProfile("user-123", failure))
                .expectErrorMatches(error -> error == failure)
                .verify();
    }
}
