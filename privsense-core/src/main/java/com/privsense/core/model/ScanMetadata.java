package com.privsense.core.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores metadata information about a database scan.
 */
@Entity
@Table(name = "scans")
public class ScanMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "connection_id")
    private UUID connectionId;
    
    @Column(name = "start_time", nullable = false)
    private Instant startTime;
    
    @Column(name = "end_time")
    private Instant endTime;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ScanStatus status;
    
    @Column(name = "database_name")
    private String databaseName;
    
    @Column(name = "database_product_name")
    private String databaseProductName;
    
    @Column(name = "database_product_version")
    private String databaseProductVersion;
    
    @Column(name = "total_tables_scanned")
    private Integer totalTablesScanned;
    
    @Column(name = "total_columns_scanned")
    private Integer totalColumnsScanned;
    
    @Column(name = "total_pii_columns_found")
    private Integer totalPiiColumnsFound;
    
    @OneToMany(mappedBy = "scanMetadata", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetectionResult> detectionResults = new ArrayList<>();
    
    @Column(name = "error_message")
    private String errorMessage;
    
    public enum ScanStatus {
        PENDING,
        EXTRACTING_METADATA,
        SAMPLING,
        DETECTING_PII,
        GENERATING_REPORT,
        COMPLETED,
        FAILED,
        CANCELLED
    }
    
    public ScanMetadata() {
    }
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getConnectionId() {
        return connectionId;
    }
    
    public void setConnectionId(UUID connectionId) {
        this.connectionId = connectionId;
    }
    
    public Instant getStartTime() {
        return startTime;
    }
    
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }
    
    public Instant getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }
    
    public ScanStatus getStatus() {
        return status;
    }
    
    public void setStatus(ScanStatus status) {
        this.status = status;
    }
    
    public String getDatabaseName() {
        return databaseName;
    }
    
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    
    public String getDatabaseProductName() {
        return databaseProductName;
    }
    
    public void setDatabaseProductName(String databaseProductName) {
        this.databaseProductName = databaseProductName;
    }
    
    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }
    
    public void setDatabaseProductVersion(String databaseProductVersion) {
        this.databaseProductVersion = databaseProductVersion;
    }
    
    public Integer getTotalTablesScanned() {
        return totalTablesScanned;
    }
    
    public void setTotalTablesScanned(Integer totalTablesScanned) {
        this.totalTablesScanned = totalTablesScanned;
    }
    
    public Integer getTotalColumnsScanned() {
        return totalColumnsScanned;
    }
    
    public void setTotalColumnsScanned(Integer totalColumnsScanned) {
        this.totalColumnsScanned = totalColumnsScanned;
    }
    
    public Integer getTotalPiiColumnsFound() {
        return totalPiiColumnsFound;
    }
    
    public void setTotalPiiColumnsFound(Integer totalPiiColumnsFound) {
        this.totalPiiColumnsFound = totalPiiColumnsFound;
    }
    
    public List<DetectionResult> getDetectionResults() {
        return detectionResults;
    }
    
    public void setDetectionResults(List<DetectionResult> detectionResults) {
        this.detectionResults = detectionResults;
        
        // Update bidirectional relationship
        if (detectionResults != null) {
            for (DetectionResult result : detectionResults) {
                result.setScanMetadata(this);
            }
        }
    }
    
    public void addDetectionResult(DetectionResult result) {
        detectionResults.add(result);
        result.setScanMetadata(this);
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "ScanMetadata{" +
                "id=" + id +
                ", connectionId=" + connectionId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", databaseName='" + databaseName + '\'' +
                ", databaseProductName='" + databaseProductName + '\'' +
                ", totalTablesScanned=" + totalTablesScanned +
                ", totalColumnsScanned=" + totalColumnsScanned +
                ", totalPiiColumnsFound=" + totalPiiColumnsFound +
                '}';
    }
}