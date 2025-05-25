package com.privsense.api.service.impl;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.notification.ScanStatusNotification;
import com.privsense.api.service.WebSocketMessagingDelegate;
import com.privsense.core.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.WebSocket;
import java.util.UUID;

/**
 * Implementation of NotificationService that sends WebSocket notifications
 * for scan status changes.
 * 
 * This service transforms ScanJobResponse objects into ScanStatusNotifications
 * that are suitable for WebSocket transmission to clients.
 */
@Slf4j
@Service
public class NotificationServiceImpl implements NotificationService {

    private final WebSocketMessagingDelegate webSocketMessagingDelegate;

    @Autowired
    public NotificationServiceImpl(WebSocketMessagingDelegate webSocketMessagingDelegate) {
        this.webSocketMessagingDelegate = webSocketMessagingDelegate;
        log.info("NotificationServiceImpl initialized with WebSocketMessagingDelegate");
    }

    /**
     * Sends a scan status update notification via WebSocket.
     * 
     * @param scanJobResponseObj The scan job response object containing status information
     */
    @Override
    public void sendScanStatusUpdate(Object scanJobResponseObj) {
        if (!(scanJobResponseObj instanceof ScanJobResponse)) {
            log.warn("Cannot send notification: input is not a ScanJobResponse");
            return;
        }

        ScanJobResponse response = (ScanJobResponse) scanJobResponseObj;
        UUID jobId = response.getJobId();
        
        if (jobId == null) {
            log.warn("Cannot send notification: jobId is null");
            return;
        }
        
        try {
            // Use a direct builder pattern approach
            // Calculate progress value before building the notification
            Integer progressValue = response.getProgress();
            if (progressValue == null && isCompletedStatus(response.getStatus())) {
                progressValue = 100;
            }
            
            ScanStatusNotification notification = ScanStatusNotification.builder()
                .jobId(jobId.toString())
                .status(response.getStatus())
                .currentOperation(response.getCurrentOperation())
                .message("Scan status updated to: " + response.getStatus())
                // Add progress information if available
                .progress(progressValue)
                // Add timestamps if available
                .startTime(response.getStartTime() != null ? response.getStartTime().toString() : null)
                .lastUpdateTime(response.getLastUpdateTime() != null ? response.getLastUpdateTime().toString() : null)
                // Add error information if available
                .errorMessage(response.getErrorMessage())
                .build();

            // Send via WebSocket
            webSocketMessagingDelegate.sendScanStatusUpdate(notification, jobId.toString());
            log.debug("Sent WebSocket notification for job {}: status={}", jobId, response.getStatus());
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for job {}: {}", jobId, e.getMessage(), e);
        }
    }
      /**
     * Determines if a status represents a completed scan.
     * 
     * @param status The scan status string
     * @return true if the status indicates completion
     */
    private boolean isCompletedStatus(String status) {
        return status != null && 
               (status.equalsIgnoreCase("COMPLETED") || 
                status.equalsIgnoreCase("FAILED") || 
                status.equalsIgnoreCase("CANCELLED"));
    }
      /**
     * Sends a job-specific notification with data payload via WebSocket.
     *
     * @param jobId The job ID to send the notification for
     * @param notificationType The type of notification (e.g., "progress", "quasi-identifiers", "results")
     * @param data The notification data payload
     */
    @Override
    public void sendJobNotification(String jobId, String notificationType, Object data) {
        if (jobId == null || notificationType == null) {
            log.warn("Cannot send job notification: jobId or notificationType is null");
            return;
        }
        
        try {
            // Send via WebSocket using the messaging delegate
            webSocketMessagingDelegate.sendJobNotification(jobId, notificationType, data);
            log.debug("Sent {} notification for job {}", notificationType, jobId);
        } catch (Exception e) {
            log.error("Failed to send {} notification for job {}: {}", notificationType, jobId, e.getMessage(), e);
        }
    }
    
    
    /**
     * Sends a direct message to a specific WebSocket destination.
     *
     * @param destination The WebSocket destination
     * @param message The message to send
     */
    @Override
    public void sendDirectMessage(String destination, Object message) {
        if (webSocketMessagingDelegate != null) {
            webSocketMessagingDelegate.sendDirectMessage(destination, message);
            log.debug("Sent direct message to destination: {}", destination);
        } else {
            log.warn("WebSocket messaging not available - direct message not sent to destination: {}", destination);
        }
    }
}