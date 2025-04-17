package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration settings for the data sampling process.
 * Used to control how data is sampled from database columns.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingConfig {
    
    /**
     * Number of rows to sample per column.
     */
    private Integer sampleSize;
    
    /**
     * Method used for sampling (e.g., "RANDOM", "FIRST_N", "STRATIFIED").
     */
    private String samplingMethod;
    
    /**
     * Maximum number of concurrent database queries allowed during sampling.
     */
    private Integer maxConcurrentQueries;
    
    /**
     * Whether to calculate entropy for sampled data.
     */
    private Boolean entropyCalculationEnabled;
}