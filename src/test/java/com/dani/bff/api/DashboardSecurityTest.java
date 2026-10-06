package com.dani.bff.api;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class DashboardSecurityTest {

    @Autowired
    private WebTestClient webTestClient;

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void invalidJwtReturnsStructuredErrorAndBearerChallenge(String token) {
        webTestClient.get()
                .uri("/api/dashboard")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueMatches(HttpHeaders.WWW_AUTHENTICATE, "Bearer.*invalid_token.*")
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.error").isEqualTo("Unauthorized")
                .jsonPath("$.message").isEqualTo("Authentication is required")
                .jsonPath("$.path").isEqualTo("/api/dashboard")
                .jsonPath("$.timestamp").exists();
    }

    private static Stream<String> invalidTokens() throws Exception {
        String secret = "test-development-secret-change-me-at-least-32-bytes";
        Instant now = Instant.now();
        return Stream.of(
                "not-a-jwt",
                signedToken("wrong-issuer", "react-dashboard", now.plusSeconds(300), secret),
                signedToken("react-bff-gateway-test", "wrong-audience", now.plusSeconds(300), secret),
                signedToken("react-bff-gateway-test", "react-dashboard", now.minusSeconds(120), secret),
                signedToken("react-bff-gateway-test", "react-dashboard", now.plusSeconds(300),
                        "different-signing-secret-at-least-32-bytes"));
    }

    private static String signedToken(String issuer, String audience, Instant expiresAt, String secret) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), new JWTClaimsSet.Builder()
                .subject("user-123")
                .issuer(issuer)
                .audience(audience)
                .expirationTime(Date.from(expiresAt))
                .build());
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    @Test
    void dashboardRequiresAuthentication() {
        webTestClient.get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith("application/json")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication is required")
                .jsonPath("$.path").isEqualTo("/api/dashboard");
    }

    @Test
    void actuatorHealthIsPublic() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").exists();
    }

    @Test
    void otherRoutesAreDeniedByDefault() {
        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/actuator/info")
                .header(HttpHeaders.ACCEPT, "application/json")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access is denied");
    }

    @Test
    void apiDocumentationRoutesAreDeniedWhenNotExplicitlyEnabled() {
        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access is denied");

        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123")))
                .get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Access is denied");
    }
}
