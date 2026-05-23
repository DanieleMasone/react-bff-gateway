package com.dani.bff.dto;

import java.util.List;
import java.util.Objects;

/**
 * Frontend-oriented dashboard payload assembled from multiple backend services.
 *
 * @param user authenticated user's display profile
 * @param recommendedProducts product recommendations for the dashboard
 */
public record DashboardResponse(UserProfile user, List<ProductRecommendation> recommendedProducts) {

    public DashboardResponse {
        Objects.requireNonNull(user, "user must not be null");
        recommendedProducts = List.copyOf(Objects.requireNonNull(recommendedProducts, "recommendedProducts must not be null"));
    }
}
