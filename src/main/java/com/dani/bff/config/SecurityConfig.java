package com.dani.bff.config;

import com.dani.bff.error.ApiError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Configures JWT resource-server security and JSON security error responses.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Protects BFF APIs, leaves health checks public, and denies unknown routes.
     *
     * @param http Spring Security reactive HTTP builder
     * @param objectMapper JSON serializer for authentication errors
     * @return the configured security filter chain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ObjectMapper objectMapper) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/api/**").authenticated()
                        .anyExchange().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper)))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Builds a JWT decoder from production JWK settings when present, otherwise from a local HMAC secret.
     *
     * @param properties externalized JWT settings
     * @return a reactive JWT decoder used by the resource server
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(JwtSecurityProperties properties) {
        if (StringUtils.hasText(properties.getIssuerUri()) && !StringUtils.hasText(properties.getJwkSetUri())
                && !StringUtils.hasText(properties.getSecret())) {
            return ReactiveJwtDecoders.fromIssuerLocation(properties.getIssuerUri());
        }

        NimbusReactiveJwtDecoder decoder;
        if (StringUtils.hasText(properties.getJwkSetUri())) {
            decoder = NimbusReactiveJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
        } else if (StringUtils.hasText(properties.getSecret())) {
            decoder = NimbusReactiveJwtDecoder.withSecretKey(secretKey(properties.getSecret()))
                    .macAlgorithm(MacAlgorithm.HS256)
                    .build();
        } else {
            throw new IllegalStateException("Configure security.jwt.jwk-set-uri, security.jwt.issuer-uri, or security.jwt.secret");
        }

        decoder.setJwtValidator(jwtValidator(properties));
        return decoder;
    }

    private static SecretKey secretKey(String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("security.jwt.secret must contain at least 32 bytes for HS256");
        }
        return new SecretKeySpec(secretBytes, "HmacSHA256");
    }

    private static OAuth2TokenValidator<Jwt> jwtValidator(JwtSecurityProperties properties) {
        OAuth2TokenValidator<Jwt> defaultValidator = StringUtils.hasText(properties.getIssuer())
                ? JwtValidators.createDefaultWithIssuer(properties.getIssuer())
                : JwtValidators.createDefault();

        if (!StringUtils.hasText(properties.getAudience())) {
            return defaultValidator;
        }

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().contains(properties.getAudience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token",
                        "JWT is missing required audience " + properties.getAudience(),
                        null));

        return new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator);
    }

    private static ServerAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (exchange, ex) -> writeJsonError(exchange, objectMapper, HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    private static ServerAccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (exchange, ex) -> writeJsonError(exchange, objectMapper, HttpStatus.FORBIDDEN, "Access is denied");
    }

    private static Mono<Void> writeJsonError(
            ServerWebExchange exchange,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ApiError error = ApiError.of(status, message, exchange.getRequest().getPath().pathWithinApplication().value());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(error);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException ex) {
            return response.setComplete();
        }
    }
}
