package com.dani.bff.gateway;

import com.dani.bff.error.DownstreamServiceException;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

final class DownstreamErrorMapper {

    private static final int MAX_BODY_LENGTH = 240;

    private DownstreamErrorMapper() {
    }

    static Mono<Throwable> toException(ClientResponse response, String serviceName) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> new DownstreamServiceException(
                        serviceName,
                        response.statusCode(),
                        summarize(body)));
    }

    private static String summarize(String body) {
        if (!StringUtils.hasText(body)) {
            return "empty response body";
        }
        return body.length() <= MAX_BODY_LENGTH ? body : body.substring(0, MAX_BODY_LENGTH) + "...";
    }
}
