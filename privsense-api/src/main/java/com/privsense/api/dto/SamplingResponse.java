package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for sampling operation response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingResponse {
    
    private String tableName;
    private String columnName;
    private int sampleSize;
    private String samplingMethod;
    private int actualRowCount;
    private int nullCount;
    private double nullPercentage;
    private double nonNullPercentage;
    private Double entropy;
    private boolean entropyCalculated;
    private long samplingTimeMs;
    private Map<String, Long> valueDistribution;
    private List<Object> sampleValues;
    
    /**
     * Additional metadata about the sampling operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigurationInfo {
        private int defaultSampleSize;
        private int maxConcurrentQueries;
        private List<String> availableSamplingMethods;
        private String defaultSamplingMethod;
    }
}