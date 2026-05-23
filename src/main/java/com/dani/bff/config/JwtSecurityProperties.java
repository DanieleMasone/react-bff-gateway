package com.dani.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT resource-server settings for local HMAC validation or production JWK validation.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtSecurityProperties {

    private String issuerUri;
    private String jwkSetUri;
    private String issuer;
    private String audience;
    private String secret;

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }
}
