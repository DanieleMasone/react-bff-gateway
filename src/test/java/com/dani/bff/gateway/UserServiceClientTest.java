package com.dani.bff.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.dani.bff.config.DownstreamProperties;
import com.dani.bff.error.DownstreamServiceException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class UserServiceClientTest {

    private MockWebServer server;
    private UserServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new UserServiceClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                properties());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchUserProfileMapsSuccessfulResponse() throws Exception {
        server.enqueue(jsonResponse("""
                {
                  "id": "user-123",
                  "firstName": "Demo",
                  "lastName": "User",
                  "email": "demo@example.com"
                }
                """));

        StepVerifier.create(client.fetchUserProfile("user-123"))
                .assertNext(user -> {
                    assertThat(user.id()).isEqualTo("user-123");
                    assertThat(user.displayName()).isEqualTo("Demo User");
                    assertThat(user.email()).isEqualTo("demo@example.com");
                })
                .verifyComplete();

        assertThat(server.takeRequest(1, TimeUnit.SECONDS).getPath()).isEqualTo("/users/user-123");
    }

    @Test
    void fetchUserProfileFailsForDownstreamServerError() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"broken\"}"));

        StepVerifier.create(client.fetchUserProfile("user-123"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DownstreamServiceException.class);
                    assertThat(error).hasMessageContaining("user-service returned HTTP 500");
                })
                .verify();
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static DownstreamProperties properties() {
        DownstreamProperties properties = new DownstreamProperties();
        DownstreamProperties.Service service = new DownstreamProperties.Service();
        service.setTimeout(Duration.ofSeconds(5));
        properties.setUserService(service);
        return properties;
    }
}
