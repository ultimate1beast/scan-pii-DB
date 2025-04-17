package com.privsense.pii.strategy;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import com.privsense.core.service.PiiDetectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import jakarta.annotation.PostConstruct;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy that uses regular expressions to detect PII patterns in sample data.
 */
@Component
@ConfigurationProperties(prefix = "privsense.detection.regex")
public class RegexStrategy implements PiiDetectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RegexStrategy.class);
    private static final String STRATEGY_NAME = "REGEX";
    
    // Maps pattern IDs to pattern definitions
    private Map<String, PatternDefinition> patternDefinitions = new HashMap<>();
    
    // Cache for compiled patterns
    private final Map<String, Pattern> compiledPatterns = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        if (patternDefinitions == null || patternDefinitions.isEmpty()) {
            logger.warn("No pattern definitions found in configuration, falling back to defaults");
            setupDefaultPatterns();
        }
        
        // Pre-compile all patterns for better performance
        for (Map.Entry<String, PatternDefinition> entry : patternDefinitions.entrySet()) {
            String patternId = entry.getKey();
            PatternDefinition definition = entry.getValue();
            
            try {
                Pattern compiledPattern = Pattern.compile(definition.getPattern());
                compiledPatterns.put(patternId, compiledPattern);
                logger.debug("Compiled pattern {}: {}", patternId, definition.getPattern());
            } catch (Exception e) {
                logger.error("Failed to compile regex pattern {}: {}", patternId, e.getMessage());
            }
        }
        
        logger.info("Initialized RegexStrategy with {} patterns", compiledPatterns.size());
    }
    
    private void setupDefaultPatterns() {
        patternDefinitions = new HashMap<>();
        
        // Email pattern
        PatternDefinition emailPattern = new PatternDefinition();
        emailPattern.setPattern("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        emailPattern.setScore(0.9);
        emailPattern.setPiiType("EMAIL");
        patternDefinitions.put("EMAIL_RFC5322", emailPattern);
        
        // US SSN pattern (XXX-XX-XXXX)
        PatternDefinition ssnPattern = new PatternDefinition();
        ssnPattern.setPattern("\\b(?!000|666|9\\d\\d)\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b");
        ssnPattern.setScore(0.95);
        ssnPattern.setPiiType("SSN");
        patternDefinitions.put("US_SSN", ssnPattern);
        
        // US Phone Number
        PatternDefinition phonePattern = new PatternDefinition();
        phonePattern.setPattern("\\b(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})\\b");
        phonePattern.setScore(0.8);
        phonePattern.setPiiType("PHONE_NUMBER");
        patternDefinitions.put("US_PHONE", phonePattern);
        
        // Credit Card Numbers
        PatternDefinition ccPattern = new PatternDefinition();
        ccPattern.setPattern("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|6(?:011|5[0-9]{2})[0-9]{12}|(?:2131|1800|35\\d{3})\\d{11})\\b");
        ccPattern.setScore(0.95);
        ccPattern.setPiiType("CREDIT_CARD_NUMBER");
        patternDefinitions.put("CREDIT_CARD", ccPattern);
        
        // Date formats (MM/DD/YYYY or YYYY-MM-DD)
        PatternDefinition datePattern = new PatternDefinition();
        datePattern.setPattern("\\b(?:0[1-9]|1[0-2])/(?:0[1-9]|[12][0-9]|3[01])/(?:19|20)\\d{2}\\b|\\b(?:19|20)\\d{2}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12][0-9]|3[01])\\b");
        datePattern.setScore(0.6); // Lower score because dates aren't always PII
        datePattern.setPiiType("DATE");
        patternDefinitions.put("DATE_FORMAT", datePattern);
        
        // IP Addresses
        PatternDefinition ipPattern = new PatternDefinition();
        ipPattern.setPattern("\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");
        ipPattern.setScore(0.7);
        ipPattern.setPiiType("IP_ADDRESS");
        patternDefinitions.put("IP_ADDRESS", ipPattern);
    }

    @Override
    public List<PiiCandidate> detect(ColumnInfo columnInfo, SampleData sampleData) {
        List<PiiCandidate> candidates = new ArrayList<>();
        
        if (columnInfo == null || sampleData == null || sampleData.getSamples() == null) {
            logger.warn("Column info or sample data is null, skipping regex detection");
            return candidates;
        }
        
        // Get non-null string samples to check
        List<String> stringSamples = sampleData.getSamples().stream()
                .filter(s -> s != null)
                .map(Object::toString)
                .collect(Collectors.toList());
        
        if (stringSamples.isEmpty()) {
            logger.debug("No string samples to analyze for column: {}", columnInfo.getColumnName());
            return candidates;
        }
        
        // Process each pattern against the samples
        for (Map.Entry<String, Pattern> entry : compiledPatterns.entrySet()) {
            String patternId = entry.getKey();
            Pattern pattern = entry.getValue();
            PatternDefinition definition = patternDefinitions.get(patternId);
            
            int matchCount = 0;
            String matchExample = null;
            
            // Check each sample against this pattern
            for (String sample : stringSamples) {
                Matcher matcher = pattern.matcher(sample);
                if (matcher.find()) {
                    matchCount++;
                    if (matchExample == null) {
                        // Store the first match as an example
                        matchExample = matcher.group();
                    }
                }
            }
            
            // Calculate match percentage and confidence score
            if (matchCount > 0) {
                double matchPercentage = (double) matchCount / stringSamples.size();
                // Base score adjusted by the percentage of matching samples
                double adjustedScore = definition.getScore() * matchPercentage;
                
                // Create evidence string
                String evidence = String.format("Pattern '%s' matched %d of %d samples (%.1f%%). Example match: '%s'", 
                        patternId, matchCount, stringSamples.size(), matchPercentage * 100, 
                        matchExample != null ? maskPii(matchExample) : "N/A");
                
                // Add candidate if the adjusted score is meaningful
                if (adjustedScore > 0.2) {  // Apply a minimum threshold
                    candidates.add(new PiiCandidate(
                            columnInfo,
                            definition.getPiiType(),
                            adjustedScore,
                            STRATEGY_NAME,
                            evidence
                    ));
                    
                    logger.debug("Added PII candidate for pattern {}: {} ({} matches, score={})", 
                            patternId, definition.getPiiType(), matchCount, adjustedScore);
                }
            }
        }
        
        return candidates;
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Masks sensitive information in the example for logging
     */
    private String maskPii(String example) {
        if (example == null || example.length() < 4) {
            return "****";
        }
        
        // Keep first and last character, mask the rest
        return example.charAt(0) + 
               "*".repeat(example.length() - 2) + 
               example.charAt(example.length() - 1);
    }
    
    public Map<String, PatternDefinition> getPatternDefinitions() {
        return patternDefinitions;
    }

    public void setPatternDefinitions(Map<String, PatternDefinition> patternDefinitions) {
        this.patternDefinitions = patternDefinitions;
    }
    
    /**
     * Inner class for pattern definition properties.
     */
    public static class PatternDefinition {
        private String pattern;
        private double score;
        private String piiType;

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public String getPiiType() {
            return piiType;
        }

        public void setPiiType(String piiType) {
            this.piiType = piiType;
        }
    }
}