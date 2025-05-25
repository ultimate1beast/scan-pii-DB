package com.privsense.pii.quasiid;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyzes the distribution of values in a column to help identify quasi-identifiers.
 * Calculates metrics such as entropy, cardinality, and frequency distributions.
 */
@Component
public class ColumnValueDistributionAnalyzer {

    /**
     * Calculates distribution metrics for a column based on sample data
     *
     * @param column The column to analyze
     * @param sampleData The sample data for the column
     * @return Distribution metrics
     */
    public ColumnDistributionMetrics calculateDistribution(ColumnInfo column, SampleData sampleData) {
        // Count occurrences of each value
        Map<Object, Integer> valueCounts = new HashMap<>();
        List<Object> samples = sampleData.getSamples();
        
        for (Object sample : samples) {
            if (sample != null) {
                valueCounts.put(sample, valueCounts.getOrDefault(sample, 0) + 1);
            }
        }
        
        int distinctValues = valueCounts.size();
        int totalSamples = samples.size();
        int singletonValues = (int) valueCounts.values().stream().filter(count -> count == 1).count();
        double distinctValueRatio = totalSamples > 0 ? (double) distinctValues / totalSamples : 0;
        double entropyValue = calculateEntropy(valueCounts, totalSamples);
        
        return new ColumnDistributionMetrics(
                distinctValues,
                totalSamples,
                distinctValueRatio,
                singletonValues,
                entropyValue,
                valueCounts);
    }
    
    /**
     * Calculates Shannon entropy for the value distribution.
     * Shannon entropy measures the uncertainty or randomness in the data.
     * Higher entropy indicates more uniform distribution and potentially more identifying power.
     *
     * @param valueCounts Map of values to their occurrence counts
     * @param totalSamples Total number of samples
     * @return Entropy value
     */
    private double calculateEntropy(Map<Object, Integer> valueCounts, int totalSamples) {
        if (totalSamples == 0) {
            return 0.0;
        }
        
        double entropy = 0.0;
        for (int count : valueCounts.values()) {
            double probability = (double) count / totalSamples;
            entropy -= probability * Math.log(probability) / Math.log(2); // Log base 2 for bits of entropy
        }
        
        return entropy;
    }
    
    /**
     * Value distribution metrics for a column.
     */
    @Data
    @AllArgsConstructor
    public static class ColumnDistributionMetrics {
        private final int distinctValueCount;
        private final int totalSampleCount;
        private final double distinctValueRatio;
        private final int singletonValueCount;
        private final double entropy;
        private final Map<Object, Integer> frequencyDistribution;
    }
}