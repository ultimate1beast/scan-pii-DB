package com.privsense.sampler.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for data sampling parameters.
 * Values are loaded from application properties with the prefix "privsense.sampling".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "privsense.sampling")
public class SamplingConfig {

    /**
     * Number of rows to fetch per column (default: 1000)
     */
    @Builder.Default
    private int sampleSize = 1000;

    /**
     * Sampling method to use (RANDOM, FIRST_N, STRATIFIED)
     */
    @Builder.Default
    private SamplingMethod samplingMethod = SamplingMethod.RANDOM;

    /**
     * Maximum number of concurrent database queries allowed
     */
    @Builder.Default
    private int maxConcurrentDbQueries = 5;

    /**
     * Whether to calculate entropy for sampled data
     */
    @Builder.Default
    private boolean entropyCalculationEnabled = false;

    /**
     * Enumeration of supported sampling methods
     */
    public enum SamplingMethod {
        RANDOM,
        FIRST_N,
        STRATIFIED
    }
}