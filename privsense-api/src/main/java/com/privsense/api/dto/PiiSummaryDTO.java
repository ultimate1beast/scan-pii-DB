package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Data Transfer Object for PII type summary information.
 * Contains summary of a specific PII type and how often it was found.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PiiSummaryDTO extends BaseResponseDTO {
    
    /**
     * The type of PII found (e.g., "EMAIL", "SSN", "CREDIT_CARD", etc.)
     */
    private String piiType;
    
    /**
     * The number of times this PII type was found across all scans
     */
    private int count;
    
    /**
     * The percentage this PII type represents of all PII findings
     */
    private double percentage;
}