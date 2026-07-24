package com.dani.bff.gateway;

import com.dani.bff.config.DownstreamProperties;
import com.dani.bff.dto.DownstreamUserResponse;
import com.dani.bff.dto.UserProfile;
import com.dani.bff.error.DownstreamServiceException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * HTTP client for the user service profile endpoint.
 */
@Component
public class UserServiceClient {

    private static final String SERVICE_NAME = "user-service";

    private final WebClient webClient;
    private final Duration timeout;

    public UserServiceClient(
            @Qualifier("userServiceWebClient") WebClient webClient,
            DownstreamProperties properties) {
        this.webClient = webClient;
        this.timeout = properties.getUserService().getTimeout();
    }

    /**
     * Fetches a user profile from the user service.
     *
     * @param userId authenticated user identifier
     * @return user profile mapped to the BFF contract
     */
    public Mono<UserProfile> fetchUserProfile(String userId) {
        return webClient.get()
                .uri("/users/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> DownstreamErrorMapper.toException(response, SERVICE_NAME))
                .bodyToMono(DownstreamUserResponse.class)
                .map(response -> response.toUserProfile(userId))
                .switchIfEmpty(Mono.error(new DownstreamServiceException(
                        SERVICE_NAME, HttpStatus.OK, "empty response body")))
                .timeout(timeout);
    }
}
