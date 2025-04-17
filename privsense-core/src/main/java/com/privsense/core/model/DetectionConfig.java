package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration settings for the PII detection process.
 * Controls thresholds and behavior of the detection pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionConfig {
    
    /**
     * Confidence threshold for the heuristic detection strategy.
     */
    private Double heuristicThreshold;
    
    /**
     * Confidence threshold for the regex detection strategy.
     */
    private Double regexThreshold;
    
    /**
     * Confidence threshold for the named entity recognition strategy.
     */
    private Double nerThreshold;
    
    /**
     * Minimum confidence threshold for reporting a PII candidate.
     */
    private Double reportingThreshold;
    
    /**
     * Whether to stop the detection pipeline when high-confidence PII is found.
     */
    private Boolean stopPipelineOnHighConfidence;
}