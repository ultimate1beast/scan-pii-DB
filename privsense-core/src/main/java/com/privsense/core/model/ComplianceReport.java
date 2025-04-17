package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Final output of the PII scanning process, containing all findings and metadata.
 * This can be serialized to JSON/HTML/PDF for reporting purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceReport {
    
    /**
     * Unique scan identifier.
     */
    private UUID scanId;
    
    /**
     * Unique identifier for the report.
     */
    private String reportId;
    
    /**
     * Summary information about the scan.
     */
    private ScanSummary summary;
    
    /**
     * Database connection information (sensitive fields masked).
     */
    private DatabaseConnectionInfo connectionInfo;
    
    /**
     * Configuration used for sampling.
     */
    private Map<String, Object> samplingConfig;
    
    /**
     * Configuration used for PII detection.
     */
    private Map<String, Object> detectionConfig;
    
    /**
     * Detailed results of PII detection.
     */
    private List<DetectionResult> detectionResults;
    
    /**
     * Timestamp when the report was generated.
     */
    private LocalDateTime generatedAt;
    
    /**
     * Database host information
     */
    private String databaseHost;
    
    /**
     * Database name
     */
    private String databaseName;
    
    /**
     * Database product name (e.g., MySQL, PostgreSQL)
     */
    private String databaseProductName;
    
    /**
     * Database product version
     */
    private String databaseProductVersion;
    
    /**
     * Total number of tables scanned
     */
    private int totalTablesScanned;
    
    /**
     * Total number of columns scanned
     */
    private int totalColumnsScanned;
    
    /**
     * Total number of PII columns found
     */
    private int totalPiiColumnsFound;
    
    /**
     * Scan start time
     */
    private Instant scanStartTime;
    
    /**
     * Scan end time
     */
    private Instant scanEndTime;
    
    /**
     * Scan duration
     */
    private Duration scanDuration;
    
    /**
     * PII findings (columns with detected PII)
     */
    private List<DetectionResult> piiFindings;
    
    /**
     * Returns a list of all PII findings (columns with detected PII).
     * 
     * @return List of detection results containing PII
     */
    public List<DetectionResult> getPiiFindings() {
        if (piiFindings != null) {
            return piiFindings;
        }
        
        if (detectionResults == null) {
            return new ArrayList<>();
        }
        
        // Filter detection results for those that have PII
        List<DetectionResult> findings = new ArrayList<>();
        for (DetectionResult result : detectionResults) {
            if (result.hasPii()) {
                findings.add(result);
            }
        }
        
        return findings;
    }
    
    /**
     * Returns a sorted list of PII findings, ordered by confidence score (highest first).
     * 
     * @return Sorted list of PII findings
     */
    public List<DetectionResult> getSortedPiiFindings() {
        List<DetectionResult> findings = getPiiFindings();
        findings.sort(Comparator.comparing(DetectionResult::getHighestConfidenceScore).reversed());
        return findings;
    }
    
    /**
     * Inner class for scan summary statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanSummary {
        private int tablesScanned;
        private int columnsScanned;
        private int piiColumnsFound;
        private int totalPiiCandidates;
        private long scanDurationMillis;
    }
}