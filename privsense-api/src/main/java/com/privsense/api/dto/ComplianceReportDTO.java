package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object pour ComplianceReport pour éviter LazyInitializationException
 * et les références circulaires lors de la sérialisation du rapport en JSON.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ComplianceReportDTO extends BaseResponseDTO {

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
    private int totalQuasiIdentifierColumnsFound;
    private Instant scanStartTime;
    private Instant scanEndTime;
    private Duration scanDuration;
    
    // Configuration used
    private Map<String, Object> samplingConfig;
    private Map<String, Object> detectionConfig;
    
    // Summary data
    private ScanSummaryDTO summary;
    
    // PII findings - using DetectionResultDTO to break circular references
    @Builder.Default
    private List<DetectionResultDTO> piiFindings = new ArrayList<>();
    
    // Quasi-identifier findings
    @Builder.Default
    private List<DetectionResultDTO> quasiIdentifierFindings = new ArrayList<>();
    
    // Correlated column groups by quasi-identifier type
    private Map<String, List<List<String>>> correlatedColumnGroups;
    
    /**
     * Inner class for scan summary statistics
     */
    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummaryDTO {
        private int tablesScanned;
        private int columnsScanned;
        private int piiColumnsFound;
        private int totalPiiCandidates;
        private int quasiIdentifierColumnsFound;
        private long scanDurationMillis;
    }
}