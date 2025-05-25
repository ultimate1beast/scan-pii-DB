package com.privsense.api.dto.notification;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for scan status notifications sent via WebSocket.
 * Contains information about scan job status changes.
 */
@Data
@Builder
public class ScanStatusNotification {
    
    private String jobId;
    private String status;
    private String currentOperation;
    private String message;
    private String startTime;
    private String lastUpdateTime;
    private String errorMessage;
    private Integer progress;
    
}