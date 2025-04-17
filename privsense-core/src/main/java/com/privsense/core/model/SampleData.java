package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds the data sampled from a specific column.
 * Contains a reference to the ColumnInfo and a List of sample values.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SampleData {
    
    private ColumnInfo columnInfo;
    
    @Builder.Default
    private List<Object> samples = new ArrayList<>();
    
    private int totalNullCount;
    private int totalRowCount;
    private Double entropy; // Optional Shannon entropy value
    
    /**
     * Adds a sample value to the list
     */
    public void addSample(Object value) {
        if (samples == null) {
            samples = new ArrayList<>();
        }
        samples.add(value);
        
        if (value == null) {
            totalNullCount++;
        }
        totalRowCount++;
    }
    
    /**
     * Returns the percentage of non-null values in the sample
     */
    public double getNonNullPercentage() {
        if (totalRowCount == 0) {
            return 0.0;
        }
        return (double) (totalRowCount - totalNullCount) / totalRowCount;
    }
    
    /**
     * Returns the percentage of null values in the sample
     */
    public double getNullPercentage() {
        if (totalRowCount == 0) {
            return 0.0;
        }
        return (double) totalNullCount / totalRowCount;
    }
    
    /**
     * Calculates the frequency distribution of values in the sample
     * @return A map with values as keys and their frequencies as values
     */
    public Map<Object, Long> getValueDistribution() {
        return samples.stream()
                .filter(s -> s != null) // Exclude nulls
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }
    
    /**
     * Calculates the Shannon entropy of the sample data if not already calculated
     * H(X) = - sum(p(x_i) * log2(p(x_i)))
     * @return The entropy value
     */
    public double calculateEntropy() {
        if (entropy != null) {
            return entropy;
        }
        
        // Get the frequency distribution
        Map<Object, Long> distribution = getValueDistribution();
        
        // Calculate entropy
        int nonNullCount = totalRowCount - totalNullCount;
        if (nonNullCount <= 1) {
            // With 0 or 1 distinct non-null values, entropy is 0
            entropy = 0.0;
            return 0.0;
        }
        
        double entropyValue = 0.0;
        for (Long frequency : distribution.values()) {
            double probability = (double) frequency / nonNullCount;
            entropyValue -= probability * (Math.log(probability) / Math.log(2));
        }
        
        entropy = entropyValue;
        return entropyValue;
    }
}