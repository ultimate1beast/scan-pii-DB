package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Entity
@Table(name = "compliance_reports")
public class ComplianceReport {
    
    /**
     * Unique scan identifier.
     */
    @Id
    @Column(name = "scan_id")
    private UUID scanId;
    
    /**
     * Unique identifier for the report.
     */
    @Column(name = "report_id")
    private String reportId;
    
    /**
     * Summary information about the scan.
     */
    @Embedded
    private ScanSummary summary;
    
    /**
     * Database connection information (sensitive fields masked).
     */
    @Transient
    private DatabaseConnectionInfo connectionInfo;
    
    /**
     * Configuration used for sampling.
     */
    @Column(name = "sampling_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> samplingConfig;
    
    /**
     * Configuration used for PII detection.
     */
    @Column(name = "detection_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> detectionConfig;
    
    /**
     * Detailed results of PII detection.
     */
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<DetectionResult> detectionResults = new ArrayList<>();
    
    /**
     * Timestamp when the report was generated.
     */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    /**
     * Database host information
     */
    @Column(name = "database_host")
    private String databaseHost;
    
    /**
     * Database name
     */
    @Column(name = "database_name")
    private String databaseName;
    
    /**
     * Database product name (e.g., MySQL, PostgreSQL)
     */
    @Column(name = "database_product_name")
    private String databaseProductName;
    
    /**
     * Database product version
     */
    @Column(name = "database_product_version")
    private String databaseProductVersion;
    
    /**
     * Total number of tables scanned
     */
    @Column(name = "total_tables_scanned")
    private int totalTablesScanned;
    
    /**
     * Total number of columns scanned
     */
    @Column(name = "total_columns_scanned")
    private int totalColumnsScanned;
    
    /**
     * Total number of PII columns found
     */
    @Column(name = "total_pii_columns_found")
    private int totalPiiColumnsFound;
    
    /**
     * Total number of quasi-identifier columns found
     */
    @Column(name = "total_qi_columns_found")
    private int totalQuasiIdentifierColumnsFound;
    
    /**
     * Scan start time
     */
    @Column(name = "scan_start_time")
    private Instant scanStartTime;
    
    /**
     * Scan end time
     */
    @Column(name = "scan_end_time")
    private Instant scanEndTime;
    
    /**
     * Scan duration
     */
    @Transient
    private Duration scanDuration;
    
    /**
     * PII findings (columns with detected PII)
     */
    @Transient
    private List<DetectionResult> piiFindings;
    
    
    /**
     * Update the scan duration based on start and end times.
     * This method should be called after setting scan start or end times.
     */
    private void updateScanDuration() {
        if (scanStartTime != null && scanEndTime != null) {
            scanDuration = Duration.between(scanStartTime, scanEndTime);
        }
    }
    
    /**
     * Initializes the transient scan duration field after loading from the database.
     * This should be called after loading a report from the database.
     */
    public void initializeScanDuration() {
        updateScanDuration();
    }

    /**
     * Set the detection results with proper bidirectional relationship management.
     * 
     * @param detectionResults The detection results to set
     */
    public void setDetectionResults(List<DetectionResult> detectionResults) {
        this.detectionResults = detectionResults;
        
        // Update bidirectional relationship
        if (detectionResults != null) {
            for (DetectionResult result : detectionResults) {
                result.setReport(this);
            }
        }
    }
    
    /**
     * Add a detection result with proper bidirectional relationship management.
     * 
     * @param result The result to add
     */
    public void addDetectionResult(DetectionResult result) {
        if (this.detectionResults == null) {
            this.detectionResults = new ArrayList<>();
        }
        this.detectionResults.add(result);
        result.setReport(this);
    }

    /**
     * Set the scan start time and update the scan duration.
     * 
     * @param scanStartTime The scan start time to set
     */
    public void setScanStartTime(Instant scanStartTime) {
        this.scanStartTime = scanStartTime;
        updateScanDuration();
    }

    /**
     * Set the scan end time and update the scan duration.
     * 
     * @param scanEndTime The scan end time to set
     */
    public void setScanEndTime(Instant scanEndTime) {
        this.scanEndTime = scanEndTime;
        updateScanDuration();
    }
    
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
    @Embeddable
    public static class ScanSummary {
        @Column(name = "tables_scanned")
        private int tablesScanned;
        
        @Column(name = "columns_scanned")
        private int columnsScanned;
        
        @Column(name = "pii_columns_found")
        private int piiColumnsFound;
        
        @Column(name = "total_pii_candidates")
        private int totalPiiCandidates;
        
        @Column(name = "qi_columns_found")
        private int quasiIdentifierColumnsFound;
        
        @Column(name = "summary_qi_groups_found")
        private int quasiIdentifierGroupsFound;
        
        @Column(name = "scan_duration_millis")
        private long scanDurationMillis;
    }
}