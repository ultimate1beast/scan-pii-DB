package com.privsense.api.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * DTO for dashboard summary statistics.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryDTO extends BaseResponseDTO {

    /**
     * Total number of database connections
     */
    private long totalConnections;

    /**
     * Total number of scans performed
     */
    private long totalScans;
    
    /**
     * Number of completed scans
     */
    private long completedScans;
    
    /**
     * Number of failed scans
     */
    private long failedScans;
    
    /**
     * Number of active scans
     */
    private long activeScans;
    
    /**
     * Total number of columns scanned
     */
    private long columnsScanned;
    
    /**
     * Total number of columns containing PII
     */
    private long totalPiiFound;
    
    /**
     * Number of databases scanned
     */
    private long databasesScanned;
    
    /**
     * Number of tables scanned
     */
    private long tablesScanned;
    
    /**
     * Percentage of columns containing PII
     */
    private double piiPercentage;
    
    /**
     * Current system status (HEALTHY, DEGRADED, etc.)
     */
    private String systemStatus;
}