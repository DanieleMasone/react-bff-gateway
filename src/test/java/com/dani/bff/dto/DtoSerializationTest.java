package com.dani.bff.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

@JsonTest
class DtoSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void dashboardResponseRoundTripsAsJson() throws Exception {
        DashboardResponse dashboard = new DashboardResponse(
                new UserProfile("user-123", "Demo User", "demo@example.com"),
                List.of(new ProductRecommendation("prd-001", "Premium Account", new BigDecimal("9.99"))));

        String json = objectMapper.writeValueAsString(dashboard);

        assertThat(json).contains("\"displayName\":\"Demo User\"");
        assertThat(json).contains("\"recommendedProducts\"");

        DashboardResponse decoded = objectMapper.readValue(json, DashboardResponse.class);
        assertThat(decoded.user().id()).isEqualTo("user-123");
        assertThat(decoded.recommendedProducts())
                .singleElement()
                .satisfies(product -> {
                    assertThat(product.id()).isEqualTo("prd-001");
                    assertThat(product.price()).isEqualByComparingTo("9.99");
                });
    }

    @Test
    void downstreamUserResponseBuildsDisplayNameFromPartsWhenNeeded() {
        DownstreamUserResponse response = new DownstreamUserResponse(
                "user-123",
                "Demo",
                "User",
                null,
                "demo@example.com");

        assertThat(response.toUserProfile("ignored").displayName()).isEqualTo("Demo User");
    }

    @Test
    void downstreamProductResponseDefaultsMissingOptionalValues() {
        ProductRecommendation recommendation = new DownstreamProductResponse(null, null, null).toRecommendation();

        assertThat(recommendation.id()).isEqualTo("unknown-product");
        assertThat(recommendation.name()).isEqualTo("Unnamed product");
        assertThat(recommendation.price()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
