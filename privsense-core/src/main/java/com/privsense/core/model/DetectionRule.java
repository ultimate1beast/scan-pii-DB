package com.privsense.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity for storing PII detection rules.
 * These rules are used for detecting various types of PII data in database columns.
 */
@Entity
@Table(name = "detection_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionRule {
    
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;
    
    @NotBlank
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @NotBlank
    @Column(name = "pattern", nullable = false, length = 1000)
    private String pattern;
    
    @NotBlank
    @Column(name = "pii_type", nullable = false)
    private String piiType;
    
    @Min(0)
    @Max(1)
    @Column(name = "confidence_score", nullable = false)
    private double confidenceScore;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "enabled", nullable = false)
    private boolean enabled;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;
    
    @Column(name = "created_at", nullable = false)
    private long createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private long updatedAt;
    
    /**
     * Enum representing the type of detection rule.
     */
    public enum RuleType {
        /**
         * Rules based on regular expressions
         */
        REGEX,
        
        /**
         * Rules based on heuristic algorithms
         */
        HEURISTIC,
        
        /**
         * Rules using Named Entity Recognition
         */
        NER
    }
    
    /**
     * Pre-persist hook to set timestamps and generate ID if needed
     */
    public void prePersist() {
        long now = System.currentTimeMillis();
        
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        
        if (createdAt == 0) {
            createdAt = now;
        }
        
        updatedAt = now;
    }
}