package com.dani.bff.gateway;

import com.dani.bff.dto.ProductRecommendation;
import com.dani.bff.dto.UserProfile;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Applies circuit-breaker policies and fallback behavior around dashboard downstream calls.
 */
@Component
public class ResilientDashboardGateway {

    private static final Logger log = LoggerFactory.getLogger(ResilientDashboardGateway.class);

    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    public ResilientDashboardGateway(UserServiceClient userServiceClient, ProductServiceClient productServiceClient) {
        this.userServiceClient = userServiceClient;
        this.productServiceClient = productServiceClient;
    }

    /**
     * Reads the user's profile with a safe placeholder fallback.
     *
     * @param userId authenticated user identifier
     * @return profile from the user service or a placeholder when unavailable
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "fallbackUserProfile")
    public Mono<UserProfile> getUserProfile(String userId) {
        return userServiceClient.fetchUserProfile(userId);
    }

    /**
     * Reads recommendations with an empty-list fallback.
     *
     * @param userId authenticated user identifier
     * @return recommendations from the product service or an empty list when unavailable
     */
    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackRecommendedProducts")
    public Mono<List<ProductRecommendation>> getRecommendedProducts(String userId) {
        return productServiceClient.fetchRecommendedProducts(userId);
    }

    Mono<UserProfile> fallbackUserProfile(String userId, Throwable cause) {
        log.warn("Using user profile fallback for userId={} because {}", userId, cause.toString());
        return Mono.just(UserProfile.unavailable(userId));
    }

    Mono<List<ProductRecommendation>> fallbackRecommendedProducts(String userId, Throwable cause) {
        log.warn("Using product recommendations fallback for userId={} because {}", userId, cause.toString());
        return Mono.just(List.of());
    }
}
