package com.privsense.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for configuring the data sampling process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingConfigDto {
    
    @NotNull(message = "Sample size is required")
    @Min(value = 1, message = "Sample size must be at least 1")
    private Integer sampleSize;
    
    @NotNull(message = "Sampling method is required")
    private String samplingMethod;
    
    @Min(value = 1, message = "Max concurrent DB queries must be at least 1")
    private Integer maxConcurrentDbQueries;
    
    private Boolean entropyCalculationEnabled;
}