package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for ComplianceReport to avoid LazyInitializationException
 * when serializing the report to JSON.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReportDTO {

    // Core metadata
    private UUID scanId;
    private String reportId;
    
    // Database info
    private String databaseHost;
    private String databaseName;
    private String databaseProductName;
    private String databaseProductVersion;
    
    // Scan statistics
    private int totalTablesScanned;
    private int totalColumnsScanned;
    private int totalPiiColumnsFound;
    private Instant scanStartTime;
    private Instant scanEndTime;
    private Duration scanDuration;
    
    // Configuration used
    private Map<String, Object> samplingConfig;
    private Map<String, Object> detectionConfig;
    
    // PII findings
    private List<PiiColumnDTO> piiFindings = new ArrayList<>();
    
    /**
     * DTO representing a column containing PII data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PiiColumnDTO {
        private String tableName;
        private String columnName;
        private String dataType;
        private String piiType;
        private double confidenceScore;
        private List<String> detectionMethods;
    }
    
    /**
     * Inner class for scan summary statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummaryDTO {
        private int tablesScanned;
        private int columnsScanned;
        private int piiColumnsFound;
        private int totalPiiCandidates;
        private long scanDurationMillis;
    }
}