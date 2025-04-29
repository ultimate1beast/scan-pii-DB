package com.privsense.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Arrays;
import java.util.List;

/**
 * Unified configuration properties for PrivSense application.
 * Binds properties prefixed with "privsense" from application.yml/properties.
 * 
 * This class centralizes all configuration properties that were previously
 * scattered across multiple modules.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "privsense")
public class PrivSenseConfigProperties {
    
    @Valid
    private final Detection detection = new Detection();
    
    @Valid
    private final Sampling sampling = new Sampling();
    
    @Valid
    private final Ner ner = new Ner();
    
    @Valid
    private final Db db = new Db();
    
    /**
     * PII detection configuration properties
     */
    @Data
    public static class Detection {
        private double heuristicThreshold = 0.7;
        private double regexThreshold = 0.8;
        private double nerThreshold = 0.6;
        private double quasiIdentifierThreshold = 0.65;
        private double reportingThreshold = 0.5;
        private boolean stopPipelineOnHighConfidence = true;
        private boolean entropyEnabled = false;
        private boolean quasiIdentifierEnabled = true;
        
        @Valid
        private QuasiIdentifier quasiIdentifier = new QuasiIdentifier();
        
        @Data
        public static class QuasiIdentifier {
            private double confidenceThreshold = 0.65;
            private double maxDistinctValueRatio = 0.8;
            private int minDistinctValueCount = 3;
            private boolean correlationAnalysisEnabled = true;
            private double minCorrelationCoefficient = 0.7;
            private int maxCorrelationColumnsToAnalyze = 100;
            private double lowCardinalityThreshold = 0.05;
            private double highCardinalityThreshold = 0.8;
            
            private List<String> commonQuasiIdentifiers = Arrays.asList(
                "zip", "zip_code", "postal_code", "post_code", 
                "gender", "sex", 
                "birth_date", "date_of_birth", "dob",
                "age", "year_of_birth", "yob",
                "race", "ethnicity",
                "marital_status", "marriage_status",
                "income", "salary",
                "education", "education_level",
                "occupation", "job_title", "profession",
                "city", "state", "region", "province",
                "country", "nationality"
            );
        }
    }
    
    /**
     * Data sampling configuration properties
     */
    @Data
    public static class Sampling {
        @Min(1)
        private int defaultSize = 1000;
        
        @Min(1)
        private int maxConcurrentDbQueries = 5;
        
        private boolean entropyCalculationEnabled = false;
        
        @NotBlank
        private String defaultMethod = "RANDOM";
    }
    
    /**
     * Named Entity Recognition service configuration properties
     */
    @Data
    public static class Ner {
        @Valid
        private Service service = new Service();
        
        @Data
        public static class Service {
            @NotBlank
            private String url = "http://localhost:8000/detect-pii";
            
            @Min(1)
            private int timeoutSeconds = 30;
            
            @Min(1)
            private int maxSamples = 10;
            
            @Min(0)
            private int retryAttempts = 2;
            
            @Valid
            private CircuitBreaker circuitBreaker = new CircuitBreaker();
            
            @Data
            public static class CircuitBreaker {
                private boolean enabled = true;
                
                @Min(1)
                private int failureThreshold = 5;
                
                @Min(1)
                private int resetTimeoutSeconds = 30;
            }
        }
    }
    
    /**
     * Database configuration properties
     */
    @Data
    public static class Db {
        @Valid
        private Pool pool = new Pool();
        
        @Valid
        private Jdbc jdbc = new Jdbc();
        
        @Data
        public static class Pool {
            private long connectionTimeout = 30000; // 30 seconds
            private long idleTimeout = 600000; // 10 minutes
            private long maxLifetime = 1800000; // 30 minutes
            private int minimumIdle = 5;
            private int maximumPoolSize = 10;
        }
        
        @Data
        public static class Jdbc {
            private String driverDir = "./drivers";
        }
    }
}