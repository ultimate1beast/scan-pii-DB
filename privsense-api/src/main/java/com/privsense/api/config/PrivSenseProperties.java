package com.privsense.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for PrivSense application.
 * Binds properties prefixed with "privsense" from application.yml/properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "privsense")
public class PrivSenseProperties {

    private Sampling sampling = new Sampling();
    private Detection detection = new Detection();
    private Ner ner = new Ner();

    /**
     * Sampling configuration properties
     */
    @Data
    public static class Sampling {
        private int defaultSize = 1000;
        private int maxConcurrentDbQueries = 5;
        private Map<String, String> methods = new HashMap<>();
    }

    /**
     * PII detection configuration properties
     */
    @Data
    public static class Detection {
        private Map<String, Double> thresholds = new HashMap<>();
        private boolean stopPipelineOnHighConfidence = true;
    }

    /**
     * NER service configuration properties
     */
    @Data
    public static class Ner {
        private Service service = new Service();

        @Data
        public static class Service {
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
    }
}