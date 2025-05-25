package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing the distribution of PII types found across scans.
 * Used for visualization in dashboards, like pie charts or bar graphs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PiiDistributionDTO {
    
    /**
     * Type of PII (e.g., EMAIL, SSN, CREDIT_CARD)
     */
    private String piiType;
    
    /**
     * Count of occurrences of this PII type
     */
    private int count;
}