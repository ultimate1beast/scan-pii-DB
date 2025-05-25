package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * Data Transfer Object for dashboard response data.
 * Aggregates various metrics and data points for dashboard displays.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DashboardResponseDTO extends BaseResponseDTO {
    
    /**
     * List of recent scan jobs (typically limited to 5)
     */
    private List<ScanJobResponse> recentScans;
    
    /**
     * Summary statistics for scan jobs
     */
    private ScanSummaryDTO scanSummary;
    
    /**
     * Count of currently active database connections
     */
    private int activeConnections;
    
    /**
     * Distribution of PII types found across scans
     */
    private List<PiiDistributionDTO> piiDistribution;
}