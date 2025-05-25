package com.privsense.api.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Application configuration class.
 * Contains bean configurations for application-wide components.
 */
@Configuration
public class AppConfig {

    /**
     * Creates a RestTemplate bean with timeout configuration.
     * This is used for making HTTP requests to external services.
     *
     * @param builder The RestTemplateBuilder
     * @return A configured RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
    }
    
    /**
     * Provides build properties as an Optional bean.
     * This allows other beans to safely access build information if available.
     *
     * @return Optional BuildProperties
     */
    @Bean
    public Optional<BuildProperties> optionalBuildProperties() {
        try {
            return Optional.of(new BuildProperties(new java.util.Properties()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}