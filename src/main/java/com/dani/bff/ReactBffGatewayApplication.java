package com.dani.bff;

import com.dani.bff.config.DownstreamProperties;
import com.dani.bff.config.JwtSecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Starts the React BFF Gateway application and enables typed external configuration.
 */
@SpringBootApplication
@EnableConfigurationProperties({DownstreamProperties.class, JwtSecurityProperties.class})
public class ReactBffGatewayApplication {

    /**
     * Launches the Spring Boot runtime.
     *
     * @param args command-line arguments passed by the runtime
     */
    public static void main(String[] args) {
        SpringApplication.run(ReactBffGatewayApplication.class, args);
    }
}
