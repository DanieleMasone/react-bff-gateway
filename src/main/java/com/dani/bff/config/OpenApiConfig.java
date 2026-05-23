package com.dani.bff.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata for the BFF's public React-facing API contract.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "React BFF Gateway API",
                version = "0.1.0",
                description = """
                        Frontend-oriented API contract for the React dashboard. The BFF owns JWT validation,
                        downstream aggregation, service response adaptation, and resilience fallbacks so the browser
                        can consume one stable dashboard payload.
                        """,
                contact = @Contact(name = "react-bff-gateway maintainers"),
                license = @License(name = "MIT")),
        tags = {
                @Tag(
                        name = "Dashboard",
                        description = "Authenticated dashboard aggregation endpoints for the React application.")
        })
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT Bearer token supplied in the Authorization header.")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
