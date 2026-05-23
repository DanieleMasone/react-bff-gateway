package com.dani.bff.api;

import com.dani.bff.config.OpenApiConfig;
import com.dani.bff.dto.DashboardResponse;
import com.dani.bff.error.ApiError;
import com.dani.bff.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
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
@Tag(name = "Dashboard", description = "Frontend-oriented dashboard aggregation API.")
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
    @Operation(
            summary = "Get dashboard data",
            description = """
                    Returns the stable dashboard model consumed by the React app. The BFF derives the user id from the
                    authenticated JWT, calls the downstream user and product services through WebClient, and shields the
                    frontend from downstream response shapes. If the user service fails, a safe placeholder profile is
                    returned. If the product service fails, recommendations are returned as an empty list.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dashboard data assembled for the authenticated user.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DashboardResponse.class),
                            examples = @ExampleObject(
                                    name = "dashboard",
                                    summary = "Dashboard response",
                                    value = """
                                            {
                                              "user": {
                                                "id": "user-123",
                                                "displayName": "Demo User",
                                                "email": "demo@example.com"
                                              },
                                              "recommendedProducts": [
                                                {
                                                  "id": "prd-001",
                                                  "name": "Premium Account",
                                                  "price": 9.99
                                                }
                                              ]
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Bearer JWT is missing, expired, malformed, or fails validation.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "unauthorized",
                                    summary = "Missing token",
                                    value = """
                                            {
                                              "timestamp": "2026-05-23T11:24:56.339Z",
                                              "status": 401,
                                              "error": "Unauthorized",
                                              "message": "Authentication is required",
                                              "path": "/api/dashboard"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "403",
                    description = "The authenticated principal is not allowed to access the resource.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "forbidden",
                                    summary = "Access denied",
                                    value = """
                                            {
                                              "timestamp": "2026-05-23T11:24:56.339Z",
                                              "status": 403,
                                              "error": "Forbidden",
                                              "message": "Access is denied",
                                              "path": "/api/dashboard"
                                            }
                                            """))),
            @ApiResponse(
                    responseCode = "502",
                    description = "A downstream failure escaped the configured resilience fallback boundary.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "downstream-failure",
                                    summary = "Downstream abstraction",
                                    value = """
                                            {
                                              "timestamp": "2026-05-23T11:24:56.339Z",
                                              "status": 502,
                                              "error": "Bad Gateway",
                                              "message": "A downstream service could not complete the request",
                                              "path": "/api/dashboard"
                                            }
                                            """)))
    })
    public Mono<DashboardResponse> dashboard(@AuthenticationPrincipal Jwt jwt) {
        return dashboardService.getDashboard(jwt);
    }
}
