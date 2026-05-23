package com.dani.bff.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Product representation returned by the downstream product service.
 *
 * @param id downstream product identifier
 * @param name display name supplied by the product service
 * @param price current product price
 */
public record DownstreamProductResponse(String id, String name, BigDecimal price) {

    /**
     * Converts the downstream contract into the stable BFF response contract.
     *
     * @return product recommendation used by the React dashboard
     */
    public ProductRecommendation toRecommendation() {
        return new ProductRecommendation(
                Objects.requireNonNullElse(id, "unknown-product"),
                Objects.requireNonNullElse(name, "Unnamed product"),
                Objects.requireNonNullElse(price, BigDecimal.ZERO));
    }
}
