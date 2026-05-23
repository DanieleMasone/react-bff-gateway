package com.dani.bff.service;

import com.dani.bff.dto.DashboardResponse;
import com.dani.bff.gateway.ResilientDashboardGateway;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Coordinates the downstream calls required by the dashboard API.
 */
@Service
public class DashboardService {

    private final ResilientDashboardGateway gateway;

    public DashboardService(ResilientDashboardGateway gateway) {
        this.gateway = gateway;
    }

    /**
     * Builds the dashboard response for the authenticated JWT subject.
     *
     * @param jwt authenticated access token
     * @return dashboard response with profile and recommendations
     */
    public Mono<DashboardResponse> getDashboard(Jwt jwt) {
        String userId = StringUtils.hasText(jwt.getSubject()) ? jwt.getSubject() : "unknown-user";

        return Mono.zip(
                        gateway.getUserProfile(userId),
                        gateway.getRecommendedProducts(userId))
                .map(tuple -> new DashboardResponse(tuple.getT1(), tuple.getT2()));
    }
}
