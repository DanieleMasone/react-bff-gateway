package com.dani.bff.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Objects;

/**
 * Frontend-oriented dashboard payload assembled from multiple backend services.
 *
 * @param user authenticated user's display profile
 * @param recommendedProducts product recommendations for the dashboard
 */
@Schema(description = "Stable dashboard payload assembled for the React application.")
public record DashboardResponse(
        @Schema(description = "Display profile for the authenticated user.")
        UserProfile user,
        @ArraySchema(schema = @Schema(implementation = ProductRecommendation.class),
                arraySchema = @Schema(description = "Recommended products to render on the dashboard."))
        List<ProductRecommendation> recommendedProducts) {

    public DashboardResponse {
        Objects.requireNonNull(user, "user must not be null");
        recommendedProducts = List.copyOf(Objects.requireNonNull(recommendedProducts, "recommendedProducts must not be null"));
    }
}
