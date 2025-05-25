package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for representing application configuration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Application configuration settings")
public class ConfigurationDTO {

    @Schema(description = "PII detection configuration")
    private DetectionConfigDTO detection;
    
    @Schema(description = "Data sampling configuration")
    private SamplingConfigDTO sampling;
    
    @Schema(description = "Named Entity Recognition configuration")
    private NerConfigDTO ner;
    
    @Schema(description = "Report generation configuration")
    private ReportingConfigDTO reporting;
    
    @Schema(description = "Database connection configuration")
    private DatabaseConfigDTO database;
    
    /**
     * DTO for detection configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "PII detection configuration settings")
    public static class DetectionConfigDTO {
        
        @Schema(description = "Threshold for heuristic-based detection", example = "0.7")
        private double heuristicThreshold;
        
        @Schema(description = "Threshold for regex-based detection", example = "0.8")
        private double regexThreshold;
        
        @Schema(description = "Threshold for NER-based detection", example = "0.6")
        private double nerThreshold;
        
        @Schema(description = "Minimum threshold for reporting PII findings", example = "0.5")
        private double reportingThreshold;
        
        @Schema(description = "Whether to stop detection pipeline on high confidence matches")
        private boolean stopPipelineOnHighConfidence;
        
        @Schema(description = "Whether entropy-based detection is enabled")
        private boolean entropyEnabled;
    }
    
    /**
     * DTO for sampling configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Data sampling configuration settings")
    public static class SamplingConfigDTO {
        
        @Schema(description = "Default sample size", example = "100")
        private int defaultSize;
        
        @Schema(description = "Maximum concurrent database queries", example = "10")
        private int maxConcurrentDbQueries;
        
        @Schema(description = "Whether entropy calculation is enabled during sampling")
        private boolean entropyCalculationEnabled;
        
        @Schema(description = "Default sampling method", example = "RANDOM")
        private String defaultMethod;
    }
    
    /**
     * DTO for Named Entity Recognition configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Named Entity Recognition configuration settings")
    public static class NerConfigDTO {
        
        @Schema(description = "Whether NER is enabled")
        private boolean enabled;
        
        @Schema(description = "NER service URL", example = "http://localhost:5000/detect-pii")
        private String serviceUrl;
        
        @Schema(description = "Timeout for NER requests in seconds", example = "30")
        private int timeoutSeconds;
        
        @Schema(description = "Maximum samples to send to NER service", example = "100")
        private int maxSamples;
        
        @Schema(description = "Number of retry attempts for NER requests", example = "2")
        private int retryAttempts;
        
        @Schema(description = "Circuit breaker configuration for NER service")
        private CircuitBreakerConfigDTO circuitBreaker;
        
        /**
         * DTO for circuit breaker configuration.
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Circuit breaker configuration settings")
        public static class CircuitBreakerConfigDTO {
            
            @Schema(description = "Whether circuit breaker is enabled")
            private boolean enabled;
            
            @Schema(description = "Failure threshold before opening circuit", example = "5")
            private int failureThreshold;
            
            @Schema(description = "Reset timeout in seconds", example = "30")
            private int resetTimeoutSeconds;
        }
    }
    
    /**
     * DTO for reporting configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Report generation configuration settings")
    public static class ReportingConfigDTO {
        
        @Schema(description = "Whether PDF report format is enabled")
        private boolean pdfEnabled;
        
        @Schema(description = "Whether CSV report format is enabled")
        private boolean csvEnabled;
        
        @Schema(description = "Whether text report format is enabled")
        private boolean textEnabled;
        
        @Schema(description = "Path where reports are saved", example = "./reports")
        private String reportOutputPath;
    }
    
    /**
     * DTO for database configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Database configuration settings")
    public static class DatabaseConfigDTO {
        
        @Schema(description = "Connection pool configuration")
        private PoolConfigDTO pool;
        
        @Schema(description = "Directory for JDBC drivers", example = "./drivers")
        private String driverDir;
        
        /**
         * DTO for connection pool configuration.
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @Schema(description = "Connection pool configuration settings")
        public static class PoolConfigDTO {
            
            @Schema(description = "Connection timeout in milliseconds", example = "30000")
            private long connectionTimeout;
            
            @Schema(description = "Idle timeout in milliseconds", example = "600000")
            private long idleTimeout;
            
            @Schema(description = "Maximum connection lifetime in milliseconds", example = "1800000")
            private long maxLifetime;
            
            @Schema(description = "Minimum idle connections", example = "5")
            private int minimumIdle;
            
            @Schema(description = "Maximum pool size", example = "10")
            private int maximumPoolSize;
        }
    }
}