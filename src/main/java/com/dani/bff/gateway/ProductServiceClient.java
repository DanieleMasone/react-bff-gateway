package com.dani.bff.gateway;

import com.dani.bff.config.DownstreamProperties;
import com.dani.bff.dto.DownstreamProductResponse;
import com.dani.bff.dto.ProductRecommendation;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for the product service recommendation endpoint.
 */
@Component
public class ProductServiceClient {

    private static final String SERVICE_NAME = "product-service";

    private final WebClient webClient;
    private final Duration timeout;

    public ProductServiceClient(
            @Qualifier("productServiceWebClient") WebClient webClient,
            DownstreamProperties properties) {
        this.webClient = webClient;
        this.timeout = properties.getProductService().getTimeout();
    }

    /**
     * Fetches product recommendations for a user from the product service.
     *
     * @param userId authenticated user identifier
     * @return recommendations supplied by the downstream service
     */
    public Mono<List<ProductRecommendation>> fetchRecommendedProducts(String userId) {
        return webClient.get()
                .uri("/users/{userId}/recommendations", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> DownstreamErrorMapper.toException(response, SERVICE_NAME))
                .bodyToFlux(DownstreamProductResponse.class)
                .map(DownstreamProductResponse::toRecommendation)
                .collectList()
                .timeout(timeout);
    }
}
