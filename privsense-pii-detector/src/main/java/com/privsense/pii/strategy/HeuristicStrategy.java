package com.privsense.pii.strategy;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import com.privsense.core.service.PiiDetectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

/**
 * Strategy that analyzes column metadata (name, comments) for PII-related keywords.
 */
@Component
public class HeuristicStrategy implements PiiDetectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HeuristicStrategy.class);
    private static final String STRATEGY_NAME = "HEURISTIC";

    // Map of keywords to confidence scores
    private final Map<String, Double> piiKeywords = new HashMap<>();

    @Value("${privsense.detection.heuristic.keywords.ssn:0.9}")
    private double ssnScore;

    @Value("${privsense.detection.heuristic.keywords.social_security:0.9}")
    private double socialSecurityScore;

    @Value("${privsense.detection.heuristic.keywords.email:0.8}")
    private double emailScore;

    @Value("${privsense.detection.heuristic.keywords.phone:0.7}")
    private double phoneScore;

    @Value("${privsense.detection.heuristic.keywords.address:0.7}")
    private double addressScore;

    @Value("${privsense.detection.heuristic.keywords.addr:0.6}")
    private double addrScore;

    @Value("${privsense.detection.heuristic.keywords.name:0.6}")
    private double nameScore;

    @Value("${privsense.detection.heuristic.keywords.nom:0.7}")
    private double nomScore;

    @Value("${privsense.detection.heuristic.keywords.dob:0.8}")
    private double dobScore;

    @Value("${privsense.detection.heuristic.keywords.birth:0.7}")
    private double birthScore;

    @Value("${privsense.detection.heuristic.keywords.passport:0.9}")
    private double passportScore;

    @Value("${privsense.detection.heuristic.keywords.license:0.8}")
    private double licenseScore;

    @Value("${privsense.detection.heuristic.keywords.credit:0.8}")
    private double creditScore;

    @Value("${privsense.detection.heuristic.keywords.card:0.5}")
    private double cardScore;

    @PostConstruct
    public void initialize() {
        // Initialize the keyword map with the injected values
        piiKeywords.put("ssn", ssnScore);
        piiKeywords.put("social_security", socialSecurityScore);
        piiKeywords.put("social security", socialSecurityScore);
        piiKeywords.put("email", emailScore);
        piiKeywords.put("mail", emailScore * 0.8);
        piiKeywords.put("phone", phoneScore);
        piiKeywords.put("telephone", phoneScore);
        piiKeywords.put("mobile", phoneScore);
        piiKeywords.put("address", addressScore);
        piiKeywords.put("addr", addrScore);
        piiKeywords.put("name", nameScore);
        piiKeywords.put("firstname", nameScore);
        piiKeywords.put("first_name", nameScore);
        piiKeywords.put("lastname", nameScore);
        piiKeywords.put("last_name", nameScore);
        piiKeywords.put("nom", nomScore);
        piiKeywords.put("prenom", nomScore);
        piiKeywords.put("dob", dobScore);
        piiKeywords.put("birth", birthScore);
        piiKeywords.put("birthdate", birthScore);
        piiKeywords.put("birth_date", birthScore);
        piiKeywords.put("date_of_birth", dobScore);
        piiKeywords.put("passport", passportScore);
        piiKeywords.put("license", licenseScore);
        piiKeywords.put("credit", creditScore);
        piiKeywords.put("card", cardScore);
        piiKeywords.put("cc_num", creditScore);
        piiKeywords.put("credit_card", creditScore);
        
        logger.info("Initialized HeuristicStrategy with {} keywords", piiKeywords.size());
    }

    @Override
    public List<PiiCandidate> detect(ColumnInfo columnInfo, SampleData sampleData) {
        List<PiiCandidate> candidates = new ArrayList<>();
        
        // Early return if column info is missing
        if (columnInfo == null) {
            logger.warn("Column info is null, skipping heuristic detection");
            return candidates;
        }
        
        String columnName = columnInfo.getColumnName().toLowerCase();
        String columnComment = columnInfo.getComments() != null ? columnInfo.getComments().toLowerCase() : "";
        
        // Check column name against keywords
        for (Map.Entry<String, Double> entry : piiKeywords.entrySet()) {
            String keyword = entry.getKey();
            double baseScore = entry.getValue();
            
            // Full match on column name gives highest confidence
            if (columnName.equals(keyword)) {
                addCandidate(candidates, columnInfo, determinePiiType(keyword), baseScore, "Exact match on column name: " + keyword);
            } 
            // Column name contains keyword
            else if (columnName.contains(keyword)) {
                // Apply a lower score for partial matches
                double adjustedScore = baseScore * 0.8;
                addCandidate(candidates, columnInfo, determinePiiType(keyword), adjustedScore, "Column name contains: " + keyword);
            }
            
            // Check comment for keyword
            if (!columnComment.isEmpty() && columnComment.contains(keyword)) {
                // Comment matches have lower confidence
                double adjustedScore = baseScore * 0.7;
                addCandidate(candidates, columnInfo, determinePiiType(keyword), adjustedScore, "Column comment contains: " + keyword);
            }
        }
        
        return candidates;
    }
    
    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    private void addCandidate(List<PiiCandidate> candidates, ColumnInfo columnInfo, String piiType, double score, String evidence) {
        PiiCandidate candidate = new PiiCandidate(columnInfo, piiType, score, STRATEGY_NAME, evidence);
        candidates.add(candidate);
        logger.debug("Added PII candidate: {}", candidate);
    }
    
    /**
     * Maps a keyword to a standardized PII type
     */
    private String determinePiiType(String keyword) {
        // Map keywords to standardized PII types
        if (keyword.contains("ssn") || keyword.contains("social_security") || keyword.contains("social security")) {
            return "SSN";
        } else if (keyword.contains("email") || keyword.contains("mail")) {
            return "EMAIL";
        } else if (keyword.contains("phone") || keyword.contains("telephone") || keyword.contains("mobile")) {
            return "PHONE_NUMBER";
        } else if (keyword.contains("address") || keyword.contains("addr")) {
            return "ADDRESS";
        } else if (keyword.contains("name") || keyword.contains("firstname") || keyword.contains("lastname") || 
                  keyword.contains("first_name") || keyword.contains("last_name") || 
                  keyword.contains("nom") || keyword.contains("prenom")) {
            return "PERSON_NAME";
        } else if (keyword.contains("dob") || keyword.contains("birth") || keyword.contains("date_of_birth")) {
            return "DATE_OF_BIRTH";
        } else if (keyword.contains("passport")) {
            return "PASSPORT_NUMBER";
        } else if (keyword.contains("license")) {
            return "LICENSE_NUMBER";
        } else if (keyword.contains("credit") || keyword.contains("card") || keyword.contains("cc_num")) {
            return "CREDIT_CARD_NUMBER";
        } else {
            return "GENERIC_PII";
        }
    }
}