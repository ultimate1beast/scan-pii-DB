package com.privsense.pii.detector;

import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.model.*;
import com.privsense.core.service.PiiDetectionStrategy;
import com.privsense.core.service.PiiDetector;
import com.privsense.core.service.PiiDetectionProgressCallback;
import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.config.QuasiIdentifierConfig;
import com.privsense.pii.quasiid.QuasiIdentifierAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of PiiDetector that orchestrates multiple PII detection strategies
 * in a configurable pipeline, including quasi-identifier detection.
 */
@Service
public class PiiDetectorImpl implements PiiDetector {

    private static final Logger logger = LoggerFactory.getLogger(PiiDetectorImpl.class);

    private final List<PiiDetectionStrategy> strategies;
    private final PrivSenseConfigProperties configProperties;
    private final QuasiIdentifierAnalyzer quasiIdentifierAnalyzer;
    private final QuasiIdentifierConfig quasiIdentifierConfig;
    
    // Cache reflection method access for better performance
    private Method setHighestConfidencePiiTypeMethod;
    private Method setHighestConfidenceScoreMethod;
    
    /**
     * Constructor with dependency injection of strategies, configuration, and the QI analyzer
     */
    @Autowired
    public PiiDetectorImpl(List<PiiDetectionStrategy> strategies, 
                          PrivSenseConfigProperties configProperties,
                          QuasiIdentifierAnalyzer quasiIdentifierAnalyzer,
                          QuasiIdentifierConfig quasiIdentifierConfig) {
        this.strategies = strategies;
        this.configProperties = configProperties;
        this.quasiIdentifierAnalyzer = quasiIdentifierAnalyzer;
        this.quasiIdentifierConfig = quasiIdentifierConfig;
        
        // Initialize reflection methods safely
        try {
            this.setHighestConfidencePiiTypeMethod = 
                DetectionResult.class.getMethod("setHighestConfidencePiiType", String.class);
            this.setHighestConfidenceScoreMethod = 
                DetectionResult.class.getMethod("setHighestConfidenceScore", double.class);
        } catch (NoSuchMethodException e) {
            logger.warn("Could not find setter methods for confidence scoring. Using fallback mechanism.");
        }
        
        logger.info("Initialized PiiDetector with {} detection strategies and quasi-identifier analysis", 
                strategies.size());
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
                if (configProperties.getDetection().isStopPipelineOnHighConfidence() && 
                    hasHighConfidenceCandidate(heuristicCandidates, configProperties.getDetection().getHeuristicThreshold())) {
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
                    if (configProperties.getDetection().isStopPipelineOnHighConfidence() && 
                        hasHighConfidenceCandidate(regexCandidates, configProperties.getDetection().getRegexThreshold())) {
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
            
            // 5. Resolve conflicts between candidates
            List<PiiCandidate> resolvedCandidates = resolveCandidateConflicts(allCandidates);
            
            // 6. Store confidence data and filter for reporting
            if (!resolvedCandidates.isEmpty()) {
                // Get highest confidence candidate
                PiiCandidate highestConfidence = resolvedCandidates.stream()
                    .max(Comparator.comparingDouble(PiiCandidate::getConfidenceScore))
                    .orElse(null);
                
                if (highestConfidence != null) {
                    // Set confidence score for all columns, including those below threshold
                    setConfidenceScore(result, highestConfidence.getPiiType(), highestConfidence.getConfidenceScore());
                    
                    // Add all candidates with their confidence scores
                    // Even if they're below reporting threshold, we'll track them
                    for (PiiCandidate candidate : resolvedCandidates) {
                        result.addCandidate(candidate);
                    }
                    
                    logger.debug("Completed PII detection for column: {}. Found {} candidate(s) with highest score {}.", 
                        columnInfo.getColumnName(), resolvedCandidates.size(), highestConfidence.getConfidenceScore());
                }
            } else {
                // No candidates found, set default values but ensure the column is tracked
                setConfidenceScore(result, "UNKNOWN", 0.0);
                logger.debug("No PII candidates found for column: {}", columnInfo.getColumnName());
            }
            
        } catch (Exception e) {
            logger.error("Error detecting PII in column: {}", columnInfo.getColumnName(), e);
            // Still ensure the column is tracked with default values
            setConfidenceScore(result, "UNKNOWN", 0.0);
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

        // Perform quasi-identifier detection if enabled - using our helper method
        if (isQuasiIdentifierDetectionEnabled()) {
            logger.info("Starting quasi-identifier detection");
            performQuasiIdentifierDetection(columnDataMap, results, null);
        } else {
            logger.info("Quasi-identifier detection is disabled by configuration");
        }

        return results;
    }
      /**
     * Detects PII in multiple columns with progress callback support.
     *
     * @param columnDataMap Map of column info to sample data
     * @param progressCallback Callback to report progress during detection
     * @return List of detection results, one for each column
     */
    @Override
    public List<DetectionResult> detectPii(Map<ColumnInfo, SampleData> columnDataMap, PiiDetectionProgressCallback progressCallback) {
        logger.info("Starting PII detection for {} columns with progress tracking", columnDataMap.size());
        
        List<DetectionResult> results = new ArrayList<>(columnDataMap.size());
        List<ColumnInfo> columnList = new ArrayList<>(columnDataMap.keySet());
        int totalColumns = columnList.size();
        
        // Update overall progress at start
        if (progressCallback != null) {
            progressCallback.onScanPhaseChanged("PII_DETECTION", "Starting PII detection for " + totalColumns + " columns");
            progressCallback.onOverallProgressUpdated(0.0, "Initializing PII detection", 0, totalColumns);
        }

        for (int i = 0; i < columnList.size(); i++) {
            ColumnInfo columnInfo = columnList.get(i);
            SampleData sampleData = columnDataMap.get(columnInfo);
            
            try {
                // Notify progress callback about column processing start
                if (progressCallback != null) {
                    progressCallback.onColumnProcessingStarted(columnInfo.getColumnName(), i, totalColumns);
                }
                
                DetectionResult result = detectPiiWithProgressCallback(columnInfo, sampleData, progressCallback, i, totalColumns);
                results.add(result);                // Notify progress callback about column processing completion
                if (progressCallback != null) {
                    boolean hasPii = result.hasPii();
                    int piiCount = result.getCandidates().size();
                    progressCallback.onColumnProcessingCompleted(columnInfo.getColumnName(), i, totalColumns, hasPii, piiCount);
                    
                    // Update overall progress
                    double progress = ((double) (i + 1) / totalColumns) * 100.0;
                    progressCallback.onOverallProgressUpdated(progress, "Processed column: " + columnInfo.getColumnName(), i + 1, totalColumns);
                }
                
            } catch (Exception e) {
                logger.error("Error detecting PII in column {}: {}", 
                        columnInfo.getColumnName(), e.getMessage());
                
                // Add empty result to maintain column mapping
                results.add(new DetectionResult(columnInfo));
            }
        }

        logger.info("Completed PII detection. Found PII in {}/{} columns", 
                results.stream().filter(DetectionResult::hasPii).count(), results.size());

        // Perform quasi-identifier detection if enabled - using our helper method
        if (isQuasiIdentifierDetectionEnabled()) {
            if (progressCallback != null) {
                progressCallback.onScanPhaseChanged("QUASI_IDENTIFIER_ANALYSIS", "Starting quasi-identifier analysis");
            }
            logger.info("Starting quasi-identifier detection");
            performQuasiIdentifierDetectionWithProgress(columnDataMap, results, null, progressCallback);
        } else {
            logger.info("Quasi-identifier detection is disabled by configuration");
        }

        return results;
    }
    
    /**
     * Detects PII in multiple columns with scan metadata for persistence
     * 
     * @param columnDataMap Map of column info to sample data
     * @param scanMetadata The scan metadata for associating results
     * @return List of detection results, one for each column
     */
    public List<DetectionResult> detectPii(Map<ColumnInfo, SampleData> columnDataMap, ScanMetadata scanMetadata) {
        // Run PII detection first without quasi-identifier analysis
        List<DetectionResult> results = detectPiiWithoutQuasiIdentifiers(columnDataMap);
        
        // Associate with scan metadata
        if (scanMetadata != null) {
            for (DetectionResult result : results) {
                result.setScanMetadata(scanMetadata);
            }
        }
        
        // Now run quasi-identifier detection with the scan metadata explicitly provided
        // This ensures the correlations are properly linked to the scan
        if (isQuasiIdentifierDetectionEnabled() && scanMetadata != null) {
            logger.info("Starting quasi-identifier detection with scan metadata {}", scanMetadata.getId());
            performQuasiIdentifierDetection(columnDataMap, results, scanMetadata);
        }
        
        return results;
    }
    
    /**
     * Internal method to perform PII detection without quasi-identifier analysis
     */
    private List<DetectionResult> detectPiiWithoutQuasiIdentifiers(Map<ColumnInfo, SampleData> columnDataMap) {
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
     * Performs quasi-identifier detection on the columns after standard PII detection
     */
    private void performQuasiIdentifierDetection(
            Map<ColumnInfo, SampleData> columnDataMap, 
            List<DetectionResult> results,
            ScanMetadata scanMetadata) {
            
        logger.info("Starting quasi-identifier detection");
        
        try {
            // Detect correlated quasi-identifier groups
            List<CorrelatedQuasiIdentifierGroup> correlatedGroups = 
                    quasiIdentifierAnalyzer.analyzeQuasiIdentifiers(columnDataMap, results, scanMetadata);
            
            // Update detection results with quasi-identifier information
            quasiIdentifierAnalyzer.updateResultsWithQuasiIdentifiers(results, correlatedGroups);
            
            int qiColumnCount = (int) results.stream().filter(DetectionResult::isQuasiIdentifier).count();
            logger.info("Completed quasi-identifier detection. Found {} QI columns in {} groups", 
                    qiColumnCount, correlatedGroups.size());
                    
        } catch (Exception e) {
            logger.error("Error during quasi-identifier detection", e);
            // Continue even if QI detection fails - we still have standard PII results
        }
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
     * Checks if quasi-identifier detection is enabled in the configuration
     * using the QuasiIdentifierConfig which contains the proper setting.
     * 
     * @return true if quasi-identifier detection is enabled
     */
    private boolean isQuasiIdentifierDetectionEnabled() {
        return quasiIdentifierConfig.isEnabled();
    }
      /**
     * Detects PII in a single column with progress callback support.
     *
     * @param columnInfo Information about the column
     * @param sampleData Sample data from the column
     * @param progressCallback Callback to report progress during detection
     * @param columnIndex Current column index for progress tracking
     * @param totalColumns Total number of columns being processed
     * @return Detection result for the column
     */
    private DetectionResult detectPiiWithProgressCallback(ColumnInfo columnInfo, SampleData sampleData, 
                                                         PiiDetectionProgressCallback progressCallback,
                                                         int columnIndex, int totalColumns) {
        logger.debug("Starting PII detection for column: {} with progress tracking", columnInfo.getColumnName());
        DetectionResult result = new DetectionResult(columnInfo);
        
        try {
            // 1. Initialize a list to collect all candidates
            List<PiiCandidate> allCandidates = new ArrayList<>();
            boolean skipRemainingStrategies = false;
            
            // 2. Execute HeuristicStrategy with progress notification
            PiiDetectionStrategy heuristicStrategy = getStrategyByName("HEURISTIC");
            if (heuristicStrategy != null) {
                List<PiiCandidate> heuristicCandidates = heuristicStrategy.detect(columnInfo, sampleData);
                allCandidates.addAll(heuristicCandidates);                  // Notify progress callback about PII detections
                for (PiiCandidate candidate : heuristicCandidates) {
                    if (progressCallback != null) {
                        progressCallback.onPiiDetected(
                            columnInfo.getColumnName(),
                            candidate.getPiiType(),
                            candidate.getConfidenceScore(),
                            "HEURISTIC",
                            columnIndex,
                            totalColumns,
                            getMaskedSample(sampleData)
                        );
                    }
                }
                
                // Check if we should continue with other strategies
                if (configProperties.getDetection().isStopPipelineOnHighConfidence() && 
                    hasHighConfidenceCandidate(heuristicCandidates, configProperties.getDetection().getHeuristicThreshold())) {
                    logger.debug("High confidence PII found by heuristic strategy, skipping remaining strategies");
                    skipRemainingStrategies = true;
                }
            }
            
            // 3. Execute RegexStrategy if needed with progress notification
            if (!skipRemainingStrategies) {
                PiiDetectionStrategy regexStrategy = getStrategyByName("REGEX");
                if (regexStrategy != null) {
                    List<PiiCandidate> regexCandidates = regexStrategy.detect(columnInfo, sampleData);
                    allCandidates.addAll(regexCandidates);
                    
                    // Notify progress callback about PII detections
                    for (PiiCandidate candidate : regexCandidates) {                        if (progressCallback != null) {
                            progressCallback.onPiiDetected(
                                columnInfo.getColumnName(),
                                candidate.getPiiType(),
                                candidate.getConfidenceScore(),
                                "REGEX",
                                columnIndex,
                                totalColumns,
                                getMaskedSample(sampleData)
                            );
                        }
                    }
                    
                    // Check if we should continue to NER
                    if (configProperties.getDetection().isStopPipelineOnHighConfidence() && 
                        hasHighConfidenceCandidate(regexCandidates, configProperties.getDetection().getRegexThreshold())) {
                        logger.debug("High confidence PII found by regex strategy, skipping NER strategy");
                        skipRemainingStrategies = true;
                    }
                }
            }
            
            // 4. Execute NerClientStrategy if needed with progress notification
            if (!skipRemainingStrategies) {
                PiiDetectionStrategy nerStrategy = getStrategyByName("NER");
                if (nerStrategy != null) {
                    List<PiiCandidate> nerCandidates = nerStrategy.detect(columnInfo, sampleData);
                    allCandidates.addAll(nerCandidates);
                      // Notify progress callback about PII detections
                    for (PiiCandidate candidate : nerCandidates) {
                        if (progressCallback != null) {
                            progressCallback.onPiiDetected(
                                columnInfo.getColumnName(),
                                candidate.getPiiType(),
                                candidate.getConfidenceScore(),
                                "NER",
                                columnIndex,
                                totalColumns,
                                getMaskedSample(sampleData)
                            );
                        }
                    }
                }
            }
            
            // 5. Resolve conflicts between candidates
            List<PiiCandidate> resolvedCandidates = resolveCandidateConflicts(allCandidates);
            
            // 6. Store confidence data and filter for reporting
            if (!resolvedCandidates.isEmpty()) {
                // Get highest confidence candidate
                PiiCandidate highestConfidence = resolvedCandidates.stream()
                    .max(Comparator.comparingDouble(PiiCandidate::getConfidenceScore))
                    .orElse(null);
                
                if (highestConfidence != null) {
                    // Set confidence score for all columns, including those below threshold
                    setConfidenceScore(result, highestConfidence.getPiiType(), highestConfidence.getConfidenceScore());
                    
                    // Add all candidates with their confidence scores
                    // Even if they're below reporting threshold, we'll track them
                    for (PiiCandidate candidate : resolvedCandidates) {
                        result.addCandidate(candidate);
                    }
                    
                    logger.debug("Completed PII detection for column: {}. Found {} candidate(s) with highest score {}.", 
                        columnInfo.getColumnName(), resolvedCandidates.size(), highestConfidence.getConfidenceScore());
                }
            } else {
                // No candidates found, set default values but ensure the column is tracked
                setConfidenceScore(result, "UNKNOWN", 0.0);
                logger.debug("No PII candidates found for column: {}", columnInfo.getColumnName());
            }
            
        } catch (Exception e) {
            logger.error("Error detecting PII in column: {}", columnInfo.getColumnName(), e);
            // Still ensure the column is tracked with default values
            setConfidenceScore(result, "UNKNOWN", 0.0);
            throw new PiiDetectionException("Error detecting PII in column " + columnInfo.getColumnName(), e);
        }
        
        return result;
    }

    /**
     * Performs quasi-identifier detection with progress callback support.
     *
     * @param columnDataMap Map of column info to sample data
     * @param results Current detection results
     * @param scanMetadata Scan metadata (can be null)
     * @param progressCallback Callback to report progress during QI analysis
     */
    private void performQuasiIdentifierDetectionWithProgress(
            Map<ColumnInfo, SampleData> columnDataMap, 
            List<DetectionResult> results,
            ScanMetadata scanMetadata,
            PiiDetectionProgressCallback progressCallback) {
            
        logger.info("Starting quasi-identifier detection with progress tracking");
        
        try {
            // Detect correlated quasi-identifier groups
            List<CorrelatedQuasiIdentifierGroup> correlatedGroups = 
                    quasiIdentifierAnalyzer.analyzeQuasiIdentifiers(columnDataMap, results, scanMetadata);
            
            // Update detection results with quasi-identifier information
            quasiIdentifierAnalyzer.updateResultsWithQuasiIdentifiers(results, correlatedGroups);
              // Notify progress callback about quasi-identifier findings
            if (progressCallback != null) {
                for (CorrelatedQuasiIdentifierGroup group : correlatedGroups) {
                    for (String columnName : group.getColumnNames()) {
                        progressCallback.onQuasiIdentifierDetected(
                            columnName,
                            group.getGroupName(), // qiGroup
                            group.getGroupType(), // qiType  
                            group.getPrivacyRiskScore(), // riskScore
                            group.getColumnNames() // correlatedColumns
                        );
                    }
                }
                  // Notify about group formation
                for (CorrelatedQuasiIdentifierGroup group : correlatedGroups) {
                    progressCallback.onQuasiIdentifierGroupFormed(
                        group.getGroupName(), // groupName
                        group.getColumnNames(), // columns
                        group.getPrivacyRiskScore(), // groupRisk
                        group.getGroupType() // detectionMethod
                    );
                }
            }
            
            int qiColumnCount = (int) results.stream().filter(DetectionResult::isQuasiIdentifier).count();
            logger.info("Completed quasi-identifier detection. Found {} QI columns in {} groups", 
                    qiColumnCount, correlatedGroups.size());
                    
        } catch (Exception e) {
            logger.error("Error during quasi-identifier detection", e);
            // Continue even if QI detection fails - we still have standard PII results
        }
    }

    /**
     * Creates a masked sample of the data for progress reporting.
     *
     * @param sampleData The original sample data
     * @return Masked sample data for secure reporting
     */
    private String getMaskedSample(SampleData sampleData) {
        if (sampleData == null || sampleData.getSampleValues() == null || sampleData.getSampleValues().isEmpty()) {
            return "[no sample data]";
        }
        
        // Take the first few values and mask them for security
        List<String> samples = sampleData.getSampleValues();
        int maxSamples = Math.min(3, samples.size());
        
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maxSamples; i++) {
            String value = samples.get(i);
            if (value != null && value.length() > 0) {
                if (value.length() <= 3) {
                    masked.append("***");
                } else {
                    masked.append(value.substring(0, 2)).append("***");
                }
            } else {
                masked.append("[empty]");
            }
            
            if (i < maxSamples - 1) {
                masked.append(", ");
            }
        }
        
        if (samples.size() > maxSamples) {
            masked.append("...");
        }
        
        return masked.toString();
    }

    /**
     * Sets the highest confidence score and PII type on a DetectionResult
     * using reflection if available, or a fallback mechanism
     * 
     * @param result The detection result to update
     * @param piiType The PII type to set
     * @param confidenceScore The confidence score to set
     */
    private void setConfidenceScore(DetectionResult result, String piiType, double confidenceScore) {
        try {
            // Try to use reflection for direct access first
            if (setHighestConfidencePiiTypeMethod != null && setHighestConfidenceScoreMethod != null) {
                setHighestConfidencePiiTypeMethod.invoke(result, piiType);
                setHighestConfidenceScoreMethod.invoke(result, confidenceScore);
                
                // Set the isPii flag based on threshold comparison
                boolean isPii = confidenceScore >= configProperties.getDetection().getReportingThreshold();
                result.setPii(isPii);
                
                // Set the "isPii" flag based on strategy-specific thresholds
                determineIsPiiByStrategy(result, confidenceScore);
                return;
            }
        } catch (Exception e) {
            logger.debug("Could not set confidence score via reflection, using fallback", e);
        }
        
        // Fallback: Use temporary candidate to trigger internal update logic
        PiiCandidate scoreTracker = new PiiCandidate(
            result.getColumnInfo(),
            piiType,
            confidenceScore,
            "CONFIDENCE_TRACKER"
        );
        
        // Add this candidate to ensure the score gets tracked, then remove it
        result.addCandidate(scoreTracker);
        result.getCandidates().remove(scoreTracker);
        
        // Explicitly set the isPii flag based on threshold
        boolean isPii = confidenceScore >= configProperties.getDetection().getReportingThreshold();
        result.setPii(isPii);
        
        // Set the "isPii" flag based on strategy-specific thresholds
        determineIsPiiByStrategy(result, confidenceScore);
    }
    
    /**
     * Determines if a column should be marked as PII based on the detection strategies used
     * and their corresponding confidence thresholds
     * 
     * @param result The detection result to update
     * @param confidenceScore The confidence score to evaluate
     */
    private void determineIsPiiByStrategy(DetectionResult result, double confidenceScore) {
        // Default to false if no candidates or methods
        boolean isPii = false;
        
        // Get detection methods from the result candidates
        List<String> detectionMethods = result.getCandidates().stream()
                .map(PiiCandidate::getDetectionMethod)
                .distinct()
                .collect(Collectors.toList());
        
        // No detection methods means no PII detected
        if (detectionMethods.isEmpty()) {
            // Check if it's a quasi-identifier - if so, mark it as sensitive data
            if (result.isQuasiIdentifier()) {
                result.setSensitiveData(true);
            } else {
                result.setPii(false); // Set both the field and attribute
            }
            return;
        }
        
        double thresholdToUse = configProperties.getDetection().getReportingThreshold(); // Default to reporting threshold
        
        // Determine the appropriate threshold based on detection methods
        if (detectionMethods.contains("NER")) {
            thresholdToUse = configProperties.getDetection().getNerThreshold();
            logger.debug("Using NER threshold: {} for column: {}", thresholdToUse, result.getColumnInfo().getColumnName());
        } else if (detectionMethods.contains("REGEX")) {
            thresholdToUse = configProperties.getDetection().getRegexThreshold();
            logger.debug("Using REGEX threshold: {} for column: {}", thresholdToUse, result.getColumnInfo().getColumnName());
        } else if (detectionMethods.contains("HEURISTIC")) {
            thresholdToUse = configProperties.getDetection().getHeuristicThreshold();
            logger.debug("Using HEURISTIC threshold: {} for column: {}", thresholdToUse, result.getColumnInfo().getColumnName());
        } else {
            logger.debug("Using default reporting threshold: {} for column: {}", thresholdToUse, result.getColumnInfo().getColumnName());
        }
        
        // A column can be both a quasi-identifier AND a direct PII if it passes the confidence threshold
        isPii = confidenceScore >= thresholdToUse;
        
        // Mark quasi-identifiers as sensitive data regardless of direct PII status
        if (result.isQuasiIdentifier()) {
            result.setSensitiveData(true);
        }
        
        // Update both the field and the attribute
        result.setPii(isPii);
        
        logger.debug("Column {} isPii={} (score={}, threshold={}, methods={}), isQI={}, isSensitive={}", 
                result.getColumnInfo().getColumnName(), 
                isPii, 
                confidenceScore,
                thresholdToUse,
                String.join(",", detectionMethods),
                result.isQuasiIdentifier(),
                result.isSensitiveData());
    }
}