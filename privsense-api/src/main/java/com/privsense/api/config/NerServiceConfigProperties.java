package com.privsense.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the external NER service.
 * Maps to properties with prefix 'privsense.ner.service' in application.yml
 */
@Component
@ConfigurationProperties(prefix = "privsense.ner.service")
@Data
public class NerServiceConfigProperties {
    private String url = "http://localhost:8000/detect-pii";
    private int timeoutSeconds = 30;
    private CircuitBreaker circuitBreaker = new CircuitBreaker();
    
    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureThreshold = 5;
        private int resetTimeoutSeconds = 30;
    }
}