package com.dani.bff.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Product card data optimized for display in the React dashboard.
 *
 * @param id stable product identifier
 * @param name display name
 * @param price current price
 */
public record ProductRecommendation(String id, String name, BigDecimal price) {

    public ProductRecommendation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(price, "price must not be null");
    }
}
