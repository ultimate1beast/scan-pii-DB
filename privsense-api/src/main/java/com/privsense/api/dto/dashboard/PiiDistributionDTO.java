package com.privsense.api.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * DTO for PII distribution data displayed on the dashboard.
 * Contains statistics about PII distribution across different dimensions.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PiiDistributionDTO extends BaseResponseDTO {

    /**
     * Distribution of PII types (e.g., EMAIL, CREDIT_CARD, SSN) and their count
     */
    private Map<String, Integer> piiTypeDistribution;
    
    /**
     * Distribution of PII by database
     */
    private Map<String, Integer> databaseDistribution;
    
    /**
     * Distribution of PII by table
     */
    private Map<String, Integer> tableDistribution;
    
    /**
     * Distribution of PII by risk level
     */
    private Map<String, Integer> riskLevelDistribution;
    
    /**
     * Top tables containing PII
     */
    private List<TablePiiSummary> topPiiTables;
    
    /**
     * Distribution of PII by detection method
     */
    private Map<String, Integer> detectionMethodDistribution;
    
    /**
     * Class representing a summary of PII found in a table
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TablePiiSummary {
        /**
         * Name of the table
         */
        private String tableName;
        
        /**
         * Count of PII columns in the table
         */
        private int piiColumnCount;
        
        /**
         * Percentage of columns that contain PII
         */
        private double piiPercentage;
        
        /**
         * Most common PII type found in this table
         */
        private String mostCommonPiiType;
    }
}