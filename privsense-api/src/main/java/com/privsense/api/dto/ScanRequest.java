package com.privsense.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object for scan requests.
 */
@Data
public class ScanRequest {
    
    @NotNull(message = "Connection ID is required")
    private UUID connectionId;
    
    private List<String> targetTables;
    
    // Sampling configuration
    @Min(value = 100, message = "Sample size must be at least 100")
    @Max(value = 10000, message = "Sample size cannot exceed 10000")
    private Integer sampleSize = 1000;
    
    private String samplingMethod = "RANDOM";
    
    private Boolean entropyCalculationEnabled = false; // Added field
    
    // Detection configuration
    @Min(value = 0, message = "Threshold must be between 0 and 1")
    @Max(value = 1, message = "Threshold must be between 0 and 1")
    private Float heuristicThreshold = 0.7f;
    
    @Min(value = 0, message = "Threshold must be between 0 and 1")
    @Max(value = 1, message = "Threshold must be between 0 and 1")
    private Float regexThreshold = 0.8f;
    
    @Min(value = 0, message = "Threshold must be between 0 and 1")
    @Max(value = 1, message = "Threshold must be between 0 and 1")
    private Float nerThreshold = 0.6f;
}