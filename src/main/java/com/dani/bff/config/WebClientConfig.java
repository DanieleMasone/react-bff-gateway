package com.dani.bff.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Provides WebClient instances scoped to each downstream service.
 */
@Configuration
public class WebClientConfig {

    @Bean
    @Qualifier("userServiceWebClient")
    public WebClient userServiceWebClient(WebClient.Builder builder, DownstreamProperties properties) {
        return builder.clone()
                .baseUrl(properties.getUserService().getBaseUrl().toString())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    @Qualifier("productServiceWebClient")
    public WebClient productServiceWebClient(WebClient.Builder builder, DownstreamProperties properties) {
        return builder.clone()
                .baseUrl(properties.getProductService().getBaseUrl().toString())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
