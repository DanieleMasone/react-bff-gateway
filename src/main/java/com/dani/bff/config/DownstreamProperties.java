package com.dani.bff.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized connection settings for downstream services consumed by the BFF.
 */
@ConfigurationProperties(prefix = "downstream")
public class DownstreamProperties {

    private Service userService = new Service();
    private Service productService = new Service();

    public Service getUserService() {
        return userService;
    }

    public void setUserService(Service userService) {
        this.userService = userService;
    }

    public Service getProductService() {
        return productService;
    }

    public void setProductService(Service productService) {
        this.productService = productService;
    }

    /**
     * Base URL and timeout for one HTTP downstream service.
     */
    public static class Service {

        private URI baseUrl;
        private Duration timeout = Duration.ofSeconds(2);

        public URI getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(URI baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
