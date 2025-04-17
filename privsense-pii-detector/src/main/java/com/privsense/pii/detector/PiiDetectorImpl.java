package com.privsense.pii.detector;

import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import com.privsense.core.service.PiiDetectionStrategy;
import com.privsense.core.service.PiiDetector;
import com.privsense.pii.config.DetectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PiiDetector that orchestrates multiple PII detection strategies
 * in a configurable pipeline.
 */
@Service
public class PiiDetectorImpl implements PiiDetector {

    private static final Logger logger = LoggerFactory.getLogger(PiiDetectorImpl.class);

    private final List<PiiDetectionStrategy> strategies;
    private final DetectionConfig config;
    
    /**
     * Constructor with dependency injection of strategies and configuration
     */
    @Autowired
    public PiiDetectorImpl(List<PiiDetectionStrategy> strategies, DetectionConfig config) {
        this.strategies = strategies;
        this.config = config;
        
        logger.info("Initialized PiiDetector with {} strategies: {}", 
                strategies.size(), 
                strategies.stream().map(PiiDetectionStrategy::getStrategyName).collect(Collectors.joining(", ")));
    }

    @Override
    public DetectionResult detectPii(ColumnInfo columnInfo, SampleData sampleData) {
        logger.debug("Starting PII detection for column: {}", columnInfo.getColumnName());
        DetectionResult result = new DetectionResult(columnInfo);
        
        try {
            // 1. Initialize a list to collect all candidates
            List<PiiCandidate> allCandidates = new ArrayList<>();
            boolean skipRemainingStrategies = false;
            
            // 2. Execute HeuristicStrategy
            PiiDetectionStrategy heuristicStrategy = getStrategyByName("HEURISTIC");
            if (heuristicStrategy != null) {
                List<PiiCandidate> heuristicCandidates = heuristicStrategy.detect(columnInfo, sampleData);
                allCandidates.addAll(heuristicCandidates);
                
                // Check if we should continue with other strategies
                if (config.isStopPipelineOnHighConfidence() && hasHighConfidenceCandidate(heuristicCandidates, config.getHeuristicThreshold())) {
                    logger.debug("High confidence PII found by heuristic strategy, skipping remaining strategies");
                    skipRemainingStrategies = true;
                }
            }
            
            // 3. Execute RegexStrategy if needed
            if (!skipRemainingStrategies) {
                PiiDetectionStrategy regexStrategy = getStrategyByName("REGEX");
                if (regexStrategy != null) {
                    List<PiiCandidate> regexCandidates = regexStrategy.detect(columnInfo, sampleData);
                    allCandidates.addAll(regexCandidates);
                    
                    // Check if we should continue to NER
                    if (config.isStopPipelineOnHighConfidence() && hasHighConfidenceCandidate(regexCandidates, config.getRegexThreshold())) {
                        logger.debug("High confidence PII found by regex strategy, skipping NER strategy");
                        skipRemainingStrategies = true;
                    }
                }
            }
            
            // 4. Execute NerClientStrategy if needed
            if (!skipRemainingStrategies) {
                PiiDetectionStrategy nerStrategy = getStrategyByName("NER");
                if (nerStrategy != null) {
                    List<PiiCandidate> nerCandidates = nerStrategy.detect(columnInfo, sampleData);
                    allCandidates.addAll(nerCandidates);
                }
            }
            
            // 5. Resolve conflicts and filter by minimum threshold
            List<PiiCandidate> resolvedCandidates = resolveCandidateConflicts(allCandidates);
            List<PiiCandidate> filteredCandidates = filterCandidatesByThreshold(resolvedCandidates, config.getMinimumReportingThreshold());
            
            // 6. Set the final candidates in the result
            filteredCandidates.forEach(result::addCandidate);
            
            logger.debug("Completed PII detection for column: {}. Found {} candidate(s) after filtering.", 
                    columnInfo.getColumnName(), filteredCandidates.size());
            
        } catch (Exception e) {
            throw new PiiDetectionException("Error detecting PII in column " + columnInfo.getColumnName(), e);
        }
        
        return result;
    }

    @Override
    public List<DetectionResult> detectPii(Map<ColumnInfo, SampleData> columnDataMap) {
        logger.info("Starting PII detection for {} columns", columnDataMap.size());
        
        List<DetectionResult> results = new ArrayList<>(columnDataMap.size());
        
        for (Map.Entry<ColumnInfo, SampleData> entry : columnDataMap.entrySet()) {
            ColumnInfo columnInfo = entry.getKey();
            SampleData sampleData = entry.getValue();
            
            try {
                DetectionResult result = detectPii(columnInfo, sampleData);
                results.add(result);
            } catch (Exception e) {
                logger.error("Error detecting PII in column {}: {}", 
                        columnInfo.getColumnName(), e.getMessage());
                
                // Add empty result to maintain column mapping
                results.add(new DetectionResult(columnInfo));
            }
        }
        
        logger.info("Completed PII detection. Found PII in {}/{} columns", 
                results.stream().filter(DetectionResult::hasPii).count(), results.size());
        
        return results;
    }
    
    /**
     * Gets a strategy by name from the injected list
     */
    private PiiDetectionStrategy getStrategyByName(String name) {
        return strategies.stream()
                .filter(s -> name.equals(s.getStrategyName()))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Checks if any candidate has a confidence score above the given threshold
     */
    private boolean hasHighConfidenceCandidate(List<PiiCandidate> candidates, double threshold) {
        return candidates.stream()
                .anyMatch(c -> c.getConfidenceScore() >= threshold);
    }
    
    /**
     * Resolves conflicts between PII candidates for the same column
     * For simplicity, keeps the candidate with the highest confidence score for each PII type
     */
    private List<PiiCandidate> resolveCandidateConflicts(List<PiiCandidate> candidates) {
        if (candidates.isEmpty()) {
            return candidates;
        }
        
        // Group by PII type
        Map<String, List<PiiCandidate>> candidatesByType = candidates.stream()
                .collect(Collectors.groupingBy(PiiCandidate::getPiiType));
        
        // For each type, keep the candidate with the highest confidence score
        List<PiiCandidate> resolved = new ArrayList<>();
        for (Map.Entry<String, List<PiiCandidate>> entry : candidatesByType.entrySet()) {
            PiiCandidate highestConfidence = entry.getValue().stream()
                    .max(Comparator.comparingDouble(PiiCandidate::getConfidenceScore))
                    .orElse(null);
            
            if (highestConfidence != null) {
                resolved.add(highestConfidence);
            }
        }
        
        return resolved;
    }
    
    /**
     * Filters candidates by a minimum confidence threshold
     */
    private List<PiiCandidate> filterCandidatesByThreshold(List<PiiCandidate> candidates, double threshold) {
        return candidates.stream()
                .filter(c -> c.getConfidenceScore() >= threshold)
                .collect(Collectors.toList());
    }
}