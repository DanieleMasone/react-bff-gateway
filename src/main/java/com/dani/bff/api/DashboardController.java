package com.dani.bff.api;

import com.dani.bff.dto.DashboardResponse;
import com.dani.bff.service.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * HTTP API dedicated to the React dashboard experience.
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Returns the dashboard model assembled for the authenticated user.
     *
     * @param jwt authenticated JWT supplied by Spring Security
     * @return a frontend-ready dashboard response
     */
    @GetMapping("/dashboard")
    public Mono<DashboardResponse> dashboard(@AuthenticationPrincipal Jwt jwt) {
        return dashboardService.getDashboard(jwt);
    }
}
