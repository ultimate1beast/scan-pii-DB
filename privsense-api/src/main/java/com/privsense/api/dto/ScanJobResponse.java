package com.privsense.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object for scan job responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanJobResponse {
    
    private UUID jobId;
    private UUID connectionId;
    private String status;
    private String currentOperation;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    private Integer progress;  // Optional, percentage complete
    private String errorMessage;  // Populated only if there was an error
    
    /**
     * Checks if the scan job is completed.
     * 
     * @return true if the job status is "COMPLETED", false otherwise
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }
}