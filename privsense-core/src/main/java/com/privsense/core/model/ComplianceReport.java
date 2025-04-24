package com.privsense.core.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;
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
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "scanId"
)
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
    @JsonManagedReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DetectionResult> detectionResults;
    
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
        
        @Column(name = "scan_duration_millis")
        private long scanDurationMillis;
    }
}