package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for representing PII detection rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "PII detection rule configuration")
public class DetectionRuleDTO {
    
    @Schema(description = "Unique identifier of the rule", example = "rule-123")
    private String id;
    
    @Schema(description = "Name of the rule", example = "Credit Card Detector")
    private String name;
    
    @Schema(description = "Pattern used for detection", example = "\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}")
    private String pattern;
    
    @Schema(description = "PII type detected by this rule", example = "CREDIT_CARD")
    private String piiType;
    
    @Schema(description = "Confidence score when this rule matches", example = "0.95")
    private double confidenceScore;
    
    @Schema(description = "Description of what the rule detects", example = "Detects credit card numbers")
    private String description;
    
    @Schema(description = "Whether this rule is active")
    private boolean enabled;
    
    @Schema(description = "Type of detection rule")
    private RuleType ruleType;
    
    /**
     * Enum representing the type of detection rule.
     */
    public enum RuleType {
        REGEX,
        HEURISTIC,
        NER
    }
}