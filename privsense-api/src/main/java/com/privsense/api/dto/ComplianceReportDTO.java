package com.privsense.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for compliance report with PII findings.
 * Restructured to eliminate duplicate data and organize information more logically.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ComplianceReportDTO extends BaseResponseDTO {
    
    /**
     * Scan information section
     */
    private ScanInfoDTO scanInfo;
    
    /**
     * Database information section
     */
    private DatabaseInfoDTO databaseInfo;
    
    /**
     * Scan summary section with count statistics
     */
    private ScanSummaryDTO scanSummary;
    
    /**
     * Summary of PII types detected, organized by category
     */
    private PiiTypesDetectedDTO piiTypesDetected;
    
    /**
     * Summary of detection methods used
     */
    private Map<String, Integer> detectionMethodsSummary;
    
    /**
     * Main section with PII findings, organized by table
     */
    private Map<String, TableFindingsDTO> tableFindings;
    
    /**
     * Scan configuration settings used
     */
    private ScanConfigurationDTO scanConfiguration;

    /**
     * Inner class for scan information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanInfoDTO {
        private String scanId;
        private String reportId;
        private Object scanStartTime;
        private Object scanEndTime;
        private String scanDuration;
    }
    
    /**
     * Inner class for database information
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfoDTO {
        private String host;
        private String name;
        private String product;
        private String version;
    }
    
    /**
     * Inner class for scan summary statistics
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummaryDTO {
        private int tablesScanned;
        private int columnsScanned;
        private int piiColumnsFound;
        private int totalPiiCandidates;
    }
    
    /**
     * Inner class for summarizing PII types found
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PiiTypesDetectedDTO {
        private Map<String, Integer> directIdentifiers;
        private Map<String, Integer> sensitiveData;
        private Map<String, Integer> dateRelated;
        private int quasiIdentifiers;
    }
    
    /**
     * Inner class for table-level findings
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableFindingsDTO {
        private List<DetectionResultDTO> columns;
    }
    
    /**
     * Inner class for scan configuration
     */
    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanConfigurationDTO {
        private Object sampling;
        private Object detection;
    }
}