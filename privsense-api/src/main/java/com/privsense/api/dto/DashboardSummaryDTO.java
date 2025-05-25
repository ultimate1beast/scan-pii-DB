package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Data Transfer Object for dashboard summary statistics.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DashboardSummaryDTO extends BaseResponseDTO {
    
    /**
     * Total number of scans in the system.
     */
    private long totalScans;
    
    /**
     * Number of successfully completed scans.
     */
    private long completedScans;
    
    /**
     * Number of failed scans.
     */
    private long failedScans;
    
    /**
     * Total number of columns scanned across all jobs.
     */
    private long totalColumnsScanned;
    
    /**
     * Total number of PII columns found across all jobs.
     */
    private long totalPiiColumnsFound;
    
    /**
     * Percentage of scanned columns that contain PII.
     */
    private double piiPercentage;
    
    /**
     * Timestamp when this summary was last updated.
     */
    private Instant lastUpdated;
}