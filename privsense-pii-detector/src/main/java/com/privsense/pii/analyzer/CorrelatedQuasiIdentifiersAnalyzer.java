package com.privsense.pii.analyzer;

import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Analyzes correlations between multiple quasi-identifiers to identify column
 * combinations that together may pose a privacy risk even if individual columns
 * do not.
 * 
 * This is essential for identifying k-anonymity vulnerabilities in large datasets.
 */
@Component
public class CorrelatedQuasiIdentifiersAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(CorrelatedQuasiIdentifiersAnalyzer.class);
    private static final String CORRELATION_STRATEGY_NAME = "QUASI_ID_CORRELATION";
    
    // Cache correlation coefficients for performance with large databases
    private final Map<CorrelationCacheKey, Double> correlationCache = new ConcurrentHashMap<>();
    
    private final PrivSenseConfigProperties configProperties;

    public CorrelatedQuasiIdentifiersAnalyzer(PrivSenseConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    /**
     * Analyzes correlations between quasi-identifiers in a set of detection results
     * and augments the results with correlation information.
     * 
     * @param results List of detection results to analyze
     * @param columnSamples Map of column data samples for analysis
     * @return Enhanced list of detection results with correlation information
     */
    public List<DetectionResult> analyzeQuasiIdentifierCorrelations(
            List<DetectionResult> results,
            Map<ColumnInfo, SampleData> columnSamples) {
        
        if (!configProperties.getDetection().getQuasiIdentifier().isCorrelationAnalysisEnabled()) {
            logger.debug("Quasi-identifier correlation analysis is disabled");
            return results;
        }
        
        // Filter only results with quasi-identifiers
        List<DetectionResult> quasiIdentifierResults = results.stream()
                .filter(this::hasQuasiIdentifierCandidate)
                .collect(Collectors.toList());
        
        if (quasiIdentifierResults.size() < 2) {
            // Need at least 2 quasi-identifiers to analyze correlations
            logger.debug("Not enough quasi-identifiers detected for correlation analysis");
            return results;
        }
        
        logger.info("Analyzing correlations between {} potential quasi-identifiers", quasiIdentifierResults.size());
        
        // Apply a limit to prevent performance issues with very large numbers of columns
        int maxColumnsToAnalyze = configProperties.getDetection().getQuasiIdentifier().getMaxCorrelationColumnsToAnalyze();
        if (quasiIdentifierResults.size() > maxColumnsToAnalyze) {
            logger.warn("Too many quasi-identifier columns ({}). Limiting analysis to {} columns with highest confidence",
                    quasiIdentifierResults.size(), maxColumnsToAnalyze);
                    
            // Sort by highest confidence score and limit
            quasiIdentifierResults = quasiIdentifierResults.stream()
                    .sorted(Comparator.comparing(DetectionResult::getHighestConfidenceScore).reversed())
                    .limit(maxColumnsToAnalyze)
                    .collect(Collectors.toList());
        }
        
        // Find correlations between quasi-identifiers
        findCorrelations(quasiIdentifierResults, columnSamples);
        
        return results;
    }

    /**
     * Finds correlations between quasi-identifier columns and adds correlation evidence
     */
    private void findCorrelations(List<DetectionResult> quasiIdentifierResults, 
                                 Map<ColumnInfo, SampleData> columnSamples) {
        double minCorrelation = configProperties.getDetection().getQuasiIdentifier().getMinCorrelationCoefficient();
        
        // For each pair of quasi-identifiers
        for (int i = 0; i < quasiIdentifierResults.size() - 1; i++) {
            DetectionResult result1 = quasiIdentifierResults.get(i);
            ColumnInfo column1 = result1.getColumnInfo();
            
            for (int j = i + 1; j < quasiIdentifierResults.size(); j++) {
                DetectionResult result2 = quasiIdentifierResults.get(j);
                ColumnInfo column2 = result2.getColumnInfo();
                
                // Skip if we cannot get samples for either column
                SampleData samples1 = columnSamples.get(column1);
                SampleData samples2 = columnSamples.get(column2);
                
                if (samples1 == null || samples2 == null || 
                    samples1.getSamples().isEmpty() || samples2.getSamples().isEmpty()) {
                    continue;
                }
                
                // Calculate correlation coefficient
                double correlation = calculateCorrelation(column1, column2, samples1, samples2);
                
                // If significant correlation found
                if (correlation >= minCorrelation) {
                    logger.debug("Found correlation of {} between {} and {}", 
                            correlation, column1.getColumnName(), column2.getColumnName());
                    
                    // Add correlation evidence to both columns
                    addCorrelationEvidence(result1, column2, correlation);
                    addCorrelationEvidence(result2, column1, correlation);
                }
            }
        }
    }
    
    /**
     * Adds a PII candidate with correlation evidence to the result
     */
    private void addCorrelationEvidence(DetectionResult result, ColumnInfo correlatedColumn, double correlation) {
        String evidence = String.format(
                "Correlated with column '%s' (correlation coefficient: %.2f). " +
                "These columns together may uniquely identify individuals.", 
                correlatedColumn.getColumnName(), correlation);
        
        // Create a PII candidate for the correlation
        PiiCandidate candidate = new PiiCandidate(
                result.getColumnInfo(),
                "QUASI_ID_CORRELATION",
                0.7 * correlation, // Scale confidence by correlation strength
                CORRELATION_STRATEGY_NAME,
                evidence
        );
        
        result.addCandidate(candidate);
    }
    
    /**
     * Calculates correlation coefficient between two columns
     */
    private double calculateCorrelation(ColumnInfo column1, ColumnInfo column2, 
                                       SampleData samples1, SampleData samples2) {
        // Try to retrieve from cache first
        CorrelationCacheKey cacheKey = new CorrelationCacheKey(column1, column2);
        if (correlationCache.containsKey(cacheKey)) {
            return correlationCache.get(cacheKey);
        }
        
        // Ensure we have the same number of samples
        int sampleSize = Math.min(samples1.getSamples().size(), samples2.getSamples().size());
        if (sampleSize < 10) {
            // Not enough samples for meaningful correlation
            return 0;
        }
        
        List<Object> col1Samples = samples1.getSamples().subList(0, sampleSize);
        List<Object> col2Samples = samples2.getSamples().subList(0, sampleSize);
        
        // For categorical data, we'll use a statistical approach to measure correlation
        Map<String, Set<String>> valueCooccurrences = new HashMap<>();
        Set<String> uniqueValues1 = new HashSet<>();
        Set<String> uniqueValues2 = new HashSet<>();
        
        // Count co-occurrences of values
        for (int i = 0; i < sampleSize; i++) {
            String val1 = col1Samples.get(i) != null ? col1Samples.get(i).toString() : "null";
            String val2 = col2Samples.get(i) != null ? col2Samples.get(i).toString() : "null";
            
            uniqueValues1.add(val1);
            uniqueValues2.add(val2);
            
            valueCooccurrences.putIfAbsent(val1, new HashSet<>());
            valueCooccurrences.get(val1).add(val2);
        }
        
        // Calculate a version of Cramer's V statistic (measure of association)
        // This is a simplification that works well for detecting related categorical fields
        int totalUniqueValues1 = uniqueValues1.size();
        int totalUniqueValues2 = uniqueValues2.size();
        
        // If either column has all unique values, correlation is low or meaningless
        if (totalUniqueValues1 == sampleSize || totalUniqueValues2 == sampleSize) {
            return 0;
        }
        
        // Average number of distinct values in col2 associated with each value in col1
        double avgDistinctValuesPerValue = 0;
        for (Set<String> associatedValues : valueCooccurrences.values()) {
            avgDistinctValuesPerValue += associatedValues.size();
        }
        avgDistinctValuesPerValue /= valueCooccurrences.size();
        
        // Calculate correlation:
        // 1 - (average distinct values per value) / (total distinct values in column 2)
        double correlation = 1 - (avgDistinctValuesPerValue / totalUniqueValues2);
        
        // Cap between 0 and 1
        correlation = Math.max(0, Math.min(1, correlation));
        
        // Store in cache
        correlationCache.put(cacheKey, correlation);
        
        return correlation;
    }
    
    /**
     * Checks if a detection result contains quasi-identifier candidates
     */
    private boolean hasQuasiIdentifierCandidate(DetectionResult result) {
        if (result.getCandidates() == null) {
            return false;
        }
        
        return result.getCandidates().stream()
                .anyMatch(candidate -> "QUASI_IDENTIFIER".equals(candidate.getDetectionMethod()) || 
                                      candidate.getPiiType().startsWith("QUASI_ID_"));
    }
    
    /**
     * Class for caching correlation calculations
     */
    private static class CorrelationCacheKey {
        private final String key;
        
        public CorrelationCacheKey(ColumnInfo col1, ColumnInfo col2) {
            // Ensure consistent key regardless of column order by sorting column names
            String[] columns = new String[] {
                    col1.getTable().getTableName() + "." + col1.getColumnName(),
                    col2.getTable().getTableName() + "." + col2.getColumnName()
            };
            Arrays.sort(columns);
            this.key = String.join("↔", columns);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CorrelationCacheKey that = (CorrelationCacheKey) o;
            return Objects.equals(key, that.key);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}