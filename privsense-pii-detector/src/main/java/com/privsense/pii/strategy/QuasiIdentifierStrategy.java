package com.privsense.pii.strategy;

import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import com.privsense.core.service.PiiDetectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Strategy for detecting quasi-identifiers in database columns.
 * 
 * Quasi-identifiers are attributes that, while not directly identifying individuals,
 * can be combined with other attributes to potentially identify individuals.
 * Examples include: zip code, birth date, gender, etc.
 */
@Component
public class QuasiIdentifierStrategy implements PiiDetectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(QuasiIdentifierStrategy.class);
    private static final String STRATEGY_NAME = "QUASI_IDENTIFIER";
    
    private final PrivSenseConfigProperties configProperties;
    
    // Maps keyword patterns to quasi-identifier types and scores
    private final Map<Pattern, QuasiIdPatternDefinition> quasiIdPatterns = new ConcurrentHashMap<>();
    
    // Cache for cardinality ratios to improve performance with large databases
    private final Map<String, Double> cardinalityCache = new ConcurrentHashMap<>();
    
    @Autowired
    public QuasiIdentifierStrategy(PrivSenseConfigProperties configProperties) {
        this.configProperties = configProperties;
    }
    
    @PostConstruct
    public void initialize() {
        setupQuasiIdPatterns();
        logger.info("Initialized QuasiIdentifierStrategy with {} patterns", quasiIdPatterns.size());
    }
    
    /**
     * Setup patterns for detecting quasi-identifiers based on column metadata
     */
    private void setupQuasiIdPatterns() {
        // Demographic quasi-identifiers
        addQuasiIdPattern("\\b(?:gender|sex)\\b", "QUASI_ID_GENDER", 0.85, 
                "Gender is a classic quasi-identifier with low cardinality");
        
        addQuasiIdPattern("\\b(?:zip|zipcode|postal[_\\s]?code|post[_\\s]?code)\\b", "QUASI_ID_ZIPCODE", 0.9, 
                "Zip/postal codes are powerful quasi-identifiers that can narrow location significantly");
        
        addQuasiIdPattern("\\b(?:age|birth[_\\s]?year|birth[_\\s]?date|year[_\\s]?of[_\\s]?birth|dob|yob)\\b", 
                "QUASI_ID_AGE_DOB", 0.85, 
                "Age/date of birth information is a classic quasi-identifier");
        
        addQuasiIdPattern("\\b(?:city|town|municipality|borough|village)\\b", "QUASI_ID_CITY", 0.8, 
                "City/town information can significantly reduce anonymity");
        
        addQuasiIdPattern("\\b(?:state|province|region|county|district|territory)\\b", "QUASI_ID_REGION", 0.7, 
                "Geographic region information contributes to re-identification risk");
        
        addQuasiIdPattern("\\b(?:country|nation)\\b", "QUASI_ID_COUNTRY", 0.6, 
                "Country information can be a quasi-identifier in international datasets");
        
        addQuasiIdPattern("\\b(?:ethnicity|race|origin|nationality)\\b", "QUASI_ID_ETHNICITY", 0.85, 
                "Ethnicity/race information is a sensitive quasi-identifier");
        
        addQuasiIdPattern("\\b(?:occupation|job|profession|position|title|role)\\b", "QUASI_ID_OCCUPATION", 0.75, 
                "Occupation information can significantly narrow identity possibilities");
        
        addQuasiIdPattern("\\b(?:education|degree|qualification|academic|diploma|certificate)\\b", "QUASI_ID_EDUCATION", 0.7, 
                "Education details contribute to quasi-identification");
        
        addQuasiIdPattern("\\b(?:marital|relationship|civil|married|single|divorced|widowed)\\b", "QUASI_ID_MARITAL_STATUS", 0.7, 
                "Marital status combined with other attributes increases re-identification risk");
        
        addQuasiIdPattern("\\b(?:income|salary|earnings|wage|compensation)\\b", "QUASI_ID_INCOME", 0.8, 
                "Income information is both sensitive and a strong quasi-identifier");
        
        addQuasiIdPattern("\\b(?:children|dependents|family[_\\s]?size|household[_\\s]?size)\\b", "QUASI_ID_HOUSEHOLD", 0.7, 
                "Family/household information contributes to re-identification");
        
        addQuasiIdPattern("\\b(?:device|browser|user[_\\s]?agent|platform|os)\\b", "QUASI_ID_DEVICE", 0.75, 
                "Device/browser information can create a digital fingerprint");
        
        addQuasiIdPattern("\\b(?:language|locale|timezone|geo|location)\\b", "QUASI_ID_LOCATION", 0.7, 
                "Language and timezone information contributes to geolocation");
    }
    
    /**
     * Adds a pattern for quasi-identifier detection
     */
    private void addQuasiIdPattern(String regex, String quasiIdType, double baseScore, String description) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        quasiIdPatterns.put(pattern, new QuasiIdPatternDefinition(quasiIdType, baseScore, description));
    }
    
    @Override
    public List<PiiCandidate> detect(ColumnInfo columnInfo, SampleData sampleData) {
        if (!configProperties.getDetection().isQuasiIdentifierEnabled()) {
            return Collections.emptyList();
        }
        
        List<PiiCandidate> candidates = new ArrayList<>();
        
        // Early return if column info is missing
        if (columnInfo == null) {
            logger.warn("Column info is null, skipping quasi-identifier detection");
            return candidates;
        }

        // Check metadata patterns
        checkColumnMetadata(columnInfo, candidates);
        
        // If we have sample data, analyze cardinality
        if (sampleData != null && sampleData.getSamples() != null && !sampleData.getSamples().isEmpty()) {
            analyzeColumnCardinality(columnInfo, sampleData, candidates);
        }
        
        return candidates;
    }

    /**
     * Checks column metadata (name, comments) against quasi-identifier patterns
     */
    private void checkColumnMetadata(ColumnInfo columnInfo, List<PiiCandidate> candidates) {
        String columnName = columnInfo.getColumnName().toLowerCase();
        String columnComment = columnInfo.getComments() != null ? columnInfo.getComments().toLowerCase() : "";
        
        for (Map.Entry<Pattern, QuasiIdPatternDefinition> entry : quasiIdPatterns.entrySet()) {
            Pattern pattern = entry.getKey();
            QuasiIdPatternDefinition definition = entry.getValue();
            double baseScore = definition.getBaseScore();
            
            // Check column name for pattern
            if (pattern.matcher(columnName).find()) {
                double score = baseScore;
                String evidence = "Column name matches quasi-identifier pattern for " + definition.getQuasiIdType() + 
                                  ". " + definition.getDescription();
                
                candidates.add(new PiiCandidate(
                    columnInfo, 
                    definition.getQuasiIdType(), 
                    score, 
                    STRATEGY_NAME, 
                    evidence
                ));
                
                // Once we've matched a pattern, continue to next column to avoid duplicates
                continue;
            }
            
            // Check comments for pattern
            if (!columnComment.isEmpty() && pattern.matcher(columnComment).find()) {
                double score = baseScore * 0.8; // Lower confidence for comment matches
                String evidence = "Column comment contains quasi-identifier pattern for " + definition.getQuasiIdType() + 
                                  ". " + definition.getDescription();
                
                candidates.add(new PiiCandidate(
                    columnInfo, 
                    definition.getQuasiIdType(), 
                    score, 
                    STRATEGY_NAME, 
                    evidence
                ));
                
                // Once we've matched a pattern, continue to next column
                continue;
            }
        }
    }
    
    /**
     * Analyzes column cardinality to identify potential quasi-identifiers
     * 
     * Columns with medium cardinality (not too unique, not too repetitive) 
     * are often good quasi-identifier candidates.
     */
    private void analyzeColumnCardinality(ColumnInfo columnInfo, SampleData sampleData, List<PiiCandidate> candidates) {
        // Skip analysis if we already identified this column as a quasi-identifier
        if (!candidates.isEmpty()) {
            return;
        }
        
        List<Object> samples = sampleData.getSamples();
        int sampleSize = samples.size();
        
        // Need reasonable number of samples for cardinality analysis
        if (sampleSize < 10) {
            return;
        }
        
        // Get unique values
        Set<String> uniqueValues = new HashSet<>();
        for (Object sample : samples) {
            if (sample != null) {
                uniqueValues.add(sample.toString());
            }
        }
        
        // Calculate cardinality ratio (unique values / total values)
        double cardinalityRatio = (double) uniqueValues.size() / sampleSize;
        
        // Cache the result for performance
        String cacheKey = columnInfo.getTable().getTableName() + "." + columnInfo.getColumnName();
        cardinalityCache.put(cacheKey, cardinalityRatio);
        
        // Ideal quasi-identifiers have medium cardinality
        double lowThreshold = configProperties.getDetection().getQuasiIdentifier().getLowCardinalityThreshold();
        double highThreshold = configProperties.getDetection().getQuasiIdentifier().getHighCardinalityThreshold();
        
        if (cardinalityRatio > lowThreshold && cardinalityRatio < highThreshold) {
            // Calculate confidence score based on how close to "ideal" medium cardinality
            // Ideal is midpoint between thresholds
            double idealCardinality = (lowThreshold + highThreshold) / 2;
            double distanceFromIdeal = Math.abs(cardinalityRatio - idealCardinality);
            double maxDistance = (highThreshold - lowThreshold) / 2;
            double confidenceScore = 0.8 * (1 - distanceFromIdeal / maxDistance);
            
            String evidence = String.format(
                "Column has medium cardinality (%.2f) which is typical of quasi-identifiers. " +
                "Found %d unique values in %d samples. Such columns often contain categorical or " +
                "demographic data that can contribute to re-identification risk when combined with other columns.",
                cardinalityRatio, uniqueValues.size(), sampleSize);
            
            candidates.add(new PiiCandidate(
                columnInfo,
                "QUASI_ID_MEDIUM_CARDINALITY",
                confidenceScore,
                STRATEGY_NAME,
                evidence
            ));
        }
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Inner class to define quasi-identifier patterns
     */
    private static class QuasiIdPatternDefinition {
        private final String quasiIdType;
        private final double baseScore;
        private final String description;
        
        public QuasiIdPatternDefinition(String quasiIdType, double baseScore, String description) {
            this.quasiIdType = quasiIdType;
            this.baseScore = baseScore;
            this.description = description;
        }
        
        public String getQuasiIdType() {
            return quasiIdType;
        }
        
        public double getBaseScore() {
            return baseScore;
        }
        
        public String getDescription() {
            return description;
        }
    }
}