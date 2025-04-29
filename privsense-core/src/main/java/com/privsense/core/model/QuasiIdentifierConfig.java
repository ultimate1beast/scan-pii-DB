package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration settings for the quasi-identifier detection process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuasiIdentifierConfig {
    
    /**
     * Whether quasi-identifier detection is enabled.
     */
    @Builder.Default
    private boolean enabled = true;
    
    /**
     * Confidence threshold for quasi-identifier detection.
     * Values below this threshold won't be reported as quasi-identifiers.
     */
    @Builder.Default
    private double confidenceThreshold = 0.5;
    
    /**
     * List of common quasi-identifier column name patterns to look for.
     */
    @Builder.Default
    private List<String> commonQuasiIdentifiers = defaultQuasiIds();
    
    /**
     * Maximum ratio of distinct values to total values for a column to be 
     * considered a quasi-identifier. Columns with higher ratios might be unique identifiers.
     */
    @Builder.Default
    private double maxDistinctValueRatio = 0.8;
    
    /**
     * Minimum number of distinct values needed for a column to be a useful quasi-identifier.
     */
    @Builder.Default
    private int minDistinctValueCount = 3;
    
    /**
     * Whether to enable analysis of correlations between quasi-identifiers.
     */
    @Builder.Default
    private boolean correlationAnalysisEnabled = true;
    
    /**
     * Minimum correlation coefficient needed to consider two columns as correlated.
     */
    @Builder.Default
    private double minCorrelationCoefficient = 0.7;
    
    /**
     * Maximum number of columns to analyze for correlation to prevent performance issues.
     */
    @Builder.Default
    private int maxCorrelationColumnsToAnalyze = 100;
    
    /**
     * Default list of common quasi-identifier column name patterns.
     */
    private static List<String> defaultQuasiIds() {
        List<String> defaults = new ArrayList<>();
        
        // Location-related patterns
        defaults.add("zip");
        defaults.add("postal_code");
        defaults.add("city");
        defaults.add("state");
        defaults.add("province");
        defaults.add("country");
        defaults.add("region");
        
        // Demographic patterns
        defaults.add("gender");
        defaults.add("sex");
        defaults.add("race");
        defaults.add("ethnicity");
        defaults.add("ethnic");
        defaults.add("age");
        defaults.add("birth_year");
        defaults.add("year_of_birth");
        
        // Professional patterns
        defaults.add("occupation");
        defaults.add("job_title");
        defaults.add("employer");
        defaults.add("industry");
        defaults.add("salary_range");
        defaults.add("income_range");
        
        // Educational patterns
        defaults.add("education");
        defaults.add("degree");
        defaults.add("school");
        defaults.add("university");
        defaults.add("graduation_year");
        defaults.add("class_year");
        
        // Medical/insurance patterns
        defaults.add("diagnosis");
        defaults.add("condition");
        defaults.add("blood_type");
        defaults.add("insurance_plan");
        defaults.add("medication");
        
        return defaults;
    }
}