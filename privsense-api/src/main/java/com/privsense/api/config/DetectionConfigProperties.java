package com.privsense.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for PII detection.
 * Maps to properties with prefix 'privsense.detection' in application.yml
 */
@Component
@ConfigurationProperties(prefix = "privsense.detection")
@Data
public class DetectionConfigProperties {
    private Thresholds thresholds = new Thresholds();
    private boolean stopPipelineOnHighConfidence = true;
    private boolean entropyEnabled = false;
    
    @Data
    public static class Thresholds {
        private double heuristic = 0.7;
        private double regex = 0.8;
        private double ner = 0.6;
        private double reporting = 0.5;
    }
}