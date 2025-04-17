package com.privsense.api.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for data sampling from database columns.
 * Maps to properties with prefix 'privsense.sampling' in application.yml
 */
@Component
@ConfigurationProperties(prefix = "privsense.sampling")
@Data
public class SamplingConfigProperties {
    private int defaultSize = 1000;
    private int maxConcurrentDbQueries = 5;
    private boolean entropyCalculationEnabled = false;
    private SamplingMethods methods = new SamplingMethods();
    
    @Data
    public static class SamplingMethods {
        private String default_ = "RANDOM";
        
        // The getter needs a special name to avoid conflict with the Java keyword
        @JsonProperty("default")
        public String getDefault() {
            return default_;
        }
        
        @JsonProperty("default")
        public void setDefault(String default_) {
            this.default_ = default_;
        }
    }
}