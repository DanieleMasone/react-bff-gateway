package com.dani.bff.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.test.StepVerifier;

class SecurityConfigTest {

    private static final String SECRET = "test-development-secret-change-me-at-least-32-bytes";

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void hmacDecoderAcceptsTokenWithExpectedIssuerAndAudience() throws Exception {
        JwtSecurityProperties properties = propertiesWithSecret();

        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(properties);

        StepVerifier.create(decoder.decode(token("user-123", "react-bff-gateway-test", "react-dashboard")))
                .assertNext(jwt -> {
                    assertThat(jwt.getSubject()).isEqualTo("user-123");
                    assertThat(jwt.getAudience()).containsExactly("react-dashboard");
                })
                .verifyComplete();
    }

    @Test
    void hmacDecoderRejectsTokenWithUnexpectedAudience() throws Exception {
        JwtSecurityProperties properties = propertiesWithSecret();

        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(properties);

        StepVerifier.create(decoder.decode(token("user-123", "react-bff-gateway-test", "other-dashboard")))
                .expectError()
                .verify();
    }

    @Test
    void hmacDecoderAllowsMissingAudienceWhenNoAudienceIsConfigured() throws Exception {
        JwtSecurityProperties properties = propertiesWithSecret();
        properties.setAudience(null);

        ReactiveJwtDecoder decoder = securityConfig.reactiveJwtDecoder(properties);

        StepVerifier.create(decoder.decode(token("user-123", "react-bff-gateway-test", null)))
                .assertNext(jwt -> assertThat(jwt.getSubject()).isEqualTo("user-123"))
                .verifyComplete();
    }

    @Test
    void jwkSetUriCreatesDecoderWithoutLocalSecret() {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setJwkSetUri("http://localhost:9000/.well-known/jwks.json");
        properties.setIssuer("react-bff-gateway-test");
        properties.setAudience("react-dashboard");

        assertThat(securityConfig.reactiveJwtDecoder(properties)).isNotNull();
    }

    @Test
    void shortHmacSecretIsRejected() {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setSecret("short");

        assertThatThrownBy(() -> securityConfig.reactiveJwtDecoder(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void missingJwtConfigurationIsRejected() {
        JwtSecurityProperties properties = new JwtSecurityProperties();

        assertThatThrownBy(() -> securityConfig.reactiveJwtDecoder(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configure security.jwt");
    }

    private static JwtSecurityProperties propertiesWithSecret() {
        JwtSecurityProperties properties = new JwtSecurityProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("react-bff-gateway-test");
        properties.setAudience("react-dashboard");
        return properties;
    }

    private static String token(String subject, String issuer, String audience) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)));
        if (audience != null) {
            claims.audience(List.of(audience));
        }

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims.build());
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
