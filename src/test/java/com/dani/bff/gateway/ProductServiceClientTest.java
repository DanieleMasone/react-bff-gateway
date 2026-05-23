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

class ProductServiceClientTest {

    private MockWebServer server;
    private ProductServiceClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new ProductServiceClient(
                WebClient.builder().baseUrl(server.url("/").toString()).build(),
                properties());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchRecommendedProductsMapsSuccessfulResponse() throws Exception {
        server.enqueue(jsonResponse("""
                [
                  {"id":"prd-001","name":"Premium Account","price":9.99},
                  {"id":"prd-002","name":"Analytics Add-on","price":19.50}
                ]
                """));

        StepVerifier.create(client.fetchRecommendedProducts("user-123"))
                .assertNext(products -> {
                    assertThat(products).hasSize(2);
                    assertThat(products.getFirst().name()).isEqualTo("Premium Account");
                    assertThat(products.getFirst().price()).isEqualByComparingTo("9.99");
                })
                .verifyComplete();

        assertThat(server.takeRequest(1, TimeUnit.SECONDS).getPath())
                .isEqualTo("/users/user-123/recommendations");
    }

    @Test
    void fetchRecommendedProductsFailsForDownstreamServerError() {
        server.enqueue(new MockResponse()
                .setResponseCode(503)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"maintenance\"}"));

        StepVerifier.create(client.fetchRecommendedProducts("user-123"))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(DownstreamServiceException.class);
                    assertThat(error).hasMessageContaining("product-service returned HTTP 503");
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
        properties.setProductService(service);
        return properties;
    }
}
