package com.privsense.api.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO for summarizing PII type counts displayed on the dashboard.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PiiSummaryDTO extends BaseResponseDTO {

    /**
     * Type of PII (e.g., EMAIL, CREDIT_CARD, SSN)
     */
    private String piiType;
    
    /**
     * Count of occurrences of this PII type
     */
    private Integer count;
    
    /**
     * Risk level associated with this PII type
     */
    private String riskLevel;
    
    /**
     * Description of this PII type
     */
    private String description;
}