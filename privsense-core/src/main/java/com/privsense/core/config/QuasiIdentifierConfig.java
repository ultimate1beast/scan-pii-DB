package com.privsense.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration settings for quasi-identifier detection feature.
 * Controls thresholds and behavior of the QI detection algorithms.
 */
@Data
@Component
@ConfigurationProperties(prefix = "privsense.detection.quasi-identifier")
public class QuasiIdentifierConfig {
    
    /**
     * Whether quasi-identifier detection is enabled
     */
    private boolean enabled = true;
    
    /**
     * Minimum confidence threshold for QI detection
     */
    private double confidenceThreshold = 0.5;
    
    /**
     * Maximum ratio of distinct values to total samples
     */
    private double maxDistinctValueRatio = 0.8;
    
    /**
     * Minimum number of distinct values required
     */
    private int minDistinctValueCount = 3;
    
    /**
     * Minimum correlation threshold between columns
     */
    // Lower this from 0.7 to 0.65 to capture more correlations
    private double correlationThreshold = 0.65;
    
    /**
     * Minimum Shannon entropy threshold
     */
    private double entropyThreshold = 0.5;
    
    /**
     * Minimum number of columns in a QI group
     */
    
    private int minGroupSize = 1;
    
    /**
     * Maximum number of columns in a QI group
     */
    private int maxGroupSize = 5;
    
    /**
     * Whether to use machine learning clustering
     */
    private boolean useMachineLearning = false;
    
    /**
     * Maximum distance threshold for DBSCAN clustering
     */
    private double clusteringDistanceThreshold = 0.3;
    
    /**
     * Threshold for high risk k-anonymity
     */
    private double kAnonymityThreshold = 5.0;
    
    /**
     * Whether to perform detailed analysis
     */
    private boolean detailedAnalysis = false;
    
    /**
     * Maximum number of column combinations to analyze
     */
    private int maxCombinationsToAnalyze = 1000;
}