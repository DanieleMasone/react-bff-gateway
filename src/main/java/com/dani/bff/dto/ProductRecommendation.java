package com.dani.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Product card data optimized for display in the React dashboard.
 *
 * @param id stable product identifier
 * @param name display name
 * @param price current price
 */
@Schema(description = "Product recommendation card data exposed to the React dashboard.")
public record ProductRecommendation(
        @Schema(description = "Stable product identifier.", example = "prd-001")
        String id,
        @Schema(description = "Display name for the product recommendation.", example = "Premium Account")
        String name,
        @Schema(description = "Current display price.", example = "9.99")
        BigDecimal price) {

    public ProductRecommendation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(price, "price must not be null");
    }
}
