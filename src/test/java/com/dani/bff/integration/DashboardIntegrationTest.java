package com.dani.bff.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.dani.bff.dto.UserProfile;
import com.dani.bff.gateway.ResilientDashboardGateway;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class DashboardIntegrationTest {

    private static final MockWebServer userServer = startServer();
    private static final MockWebServer productServer = startServer();

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ResilientDashboardGateway gateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @DynamicPropertySource
    static void downstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("downstream.user-service.base-url", () -> userServer.url("/").toString());
        registry.add("downstream.product-service.base-url", () -> productServer.url("/").toString());
    }

    @BeforeEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.circuitBreaker("userService").reset();
        circuitBreakerRegistry.circuitBreaker("productService").reset();
    }

    @AfterAll
    static void stopServers() throws IOException {
        userServer.shutdown();
        productServer.shutdown();
    }

    @Test
    void dashboardAggregatesUserProfileAndRecommendedProducts() {
        userServer.enqueue(jsonResponse("""
                {
                  "id": "user-123",
                  "displayName": "Demo User",
                  "email": "demo@example.com"
                }
                """));
        productServer.enqueue(jsonResponse("""
                [
                  {"id":"prd-001","name":"Premium Account","price":9.99}
                ]
                """));

        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.id").isEqualTo("user-123")
                .jsonPath("$.user.displayName").isEqualTo("Demo User")
                .jsonPath("$.recommendedProducts[0].id").isEqualTo("prd-001")
                .jsonPath("$.recommendedProducts[0].price").isEqualTo(9.99);
    }

    @Test
    void dashboardUsesFallbacksWhenDownstreamServicesFail() {
        userServer.enqueue(new MockResponse().setResponseCode(503).setBody("user service down"));
        productServer.enqueue(new MockResponse().setResponseCode(500).setBody("product service down"));

        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.user.id").isEqualTo("user-123")
                .jsonPath("$.user.displayName").isEqualTo("Guest User")
                .jsonPath("$.recommendedProducts").isEmpty();
    }

    @Test
    void userCircuitBreakerOpensAfterRepeatedFailuresAndShortCircuitsCalls() {
        userServer.enqueue(new MockResponse().setResponseCode(503).setBody("first failure"));
        userServer.enqueue(new MockResponse().setResponseCode(503).setBody("second failure"));

        StepVerifier.create(gateway.getUserProfile("user-123"))
                .expectNext(UserProfile.unavailable("user-123"))
                .verifyComplete();
        StepVerifier.create(gateway.getUserProfile("user-123"))
                .expectNext(UserProfile.unavailable("user-123"))
                .verifyComplete();

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("userService");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int requestCountBeforeShortCircuit = userServer.getRequestCount();
        StepVerifier.create(gateway.getUserProfile("user-123"))
                .expectNext(UserProfile.unavailable("user-123"))
                .verifyComplete();
        assertThat(userServer.getRequestCount()).isEqualTo(requestCountBeforeShortCircuit);
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static MockWebServer startServer() {
        try {
            MockWebServer server = new MockWebServer();
            server.start();
            return server;
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
