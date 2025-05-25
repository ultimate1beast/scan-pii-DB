package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Data Transfer Object for scan summary statistics.
 * Used in dashboard displays to show scan completion metrics.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ScanSummaryDTO {
    
    /**
     * Total number of scan jobs
     */
    private int totalScans;
    
    /**
     * Number of successfully completed scan jobs
     */
    private int completedScans;
    
    /**
     * Number of failed scan jobs
     */
    private int failedScans;
    
    /**
     * Number of scans currently in progress
     */
    private int inProgressScans;
    
    /**
     * Calculate and return completion rate as a percentage
     * @return Percentage of scans that completed successfully
     */
    public double getCompletionRate() {
        return totalScans > 0 ? (double) completedScans / totalScans * 100 : 0;
    }
    
    /**
     * Calculate and return failure rate as a percentage
     * @return Percentage of scans that failed
     */
    public double getFailureRate() {
        return totalScans > 0 ? (double) failedScans / totalScans * 100 : 0;
    }
}