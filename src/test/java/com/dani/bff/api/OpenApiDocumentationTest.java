package com.dani.bff.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "springdoc.api-docs.enabled=true",
                "springdoc.swagger-ui.enabled=true"
        })
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void openApiJsonDocumentsDashboardContractAndJwtSecurity() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.openapi").value(version -> assertThat(version.toString()).startsWith("3.0"))
                .jsonPath("$.info.title").isEqualTo("React BFF Gateway API")
                .jsonPath("$.components.securitySchemes.bearerAuth.type").isEqualTo("http")
                .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer")
                .jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").isEqualTo("JWT")
                .jsonPath("$.paths['/api/dashboard'].get.summary").isEqualTo("Get dashboard data")
                .jsonPath("$.paths['/api/dashboard'].get.security[0].bearerAuth").exists()
                .jsonPath("$.paths['/api/dashboard'].get.responses['200'].content['application/json'].examples.dashboard").exists()
                .jsonPath("$.paths['/api/dashboard'].get.responses['401'].content['application/json'].examples.unauthorized").exists()
                .jsonPath("$.paths['/api/dashboard'].get.responses['403'].content['application/json'].examples.forbidden").exists()
                .jsonPath("$.paths['/api/dashboard'].get.responses['502'].content['application/json'].examples['downstream-failure']").exists();
    }

    @Test
    void openApiYamlCanBeExportedForStaticDocumentation() {
        webTestClient.get()
                .uri("/v3/api-docs.yaml")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("openapi: 3.0")
                        .contains("/api/dashboard:")
                        .contains("bearerAuth:"));
    }

    @Test
    void swaggerUiIsProvidedBySpringdocWhenDocumentationIsEnabled() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader()
                .value("Location", location -> assertThat(location).contains("/swagger-ui/index.html"));

        webTestClient.get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("swagger-ui-bundle.js")
                        .contains("swagger-initializer.js"));

        webTestClient.get()
                .uri("/swagger-ui/swagger-initializer.js")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body)
                        .contains("/v3/api-docs/swagger-config")
                        .contains("persistAuthorization"));
    }
}
