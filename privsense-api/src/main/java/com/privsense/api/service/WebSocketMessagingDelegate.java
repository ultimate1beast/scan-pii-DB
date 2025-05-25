package com.privsense.api.service;

import com.privsense.api.dto.notification.ScanStatusNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Delegate class that handles WebSocket messaging operations.
 * This class will only be instantiated if WebSocket is enabled in the application.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "privsense.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketMessagingDelegate {

    private SimpMessagingTemplate messagingTemplate;
    
    // Topic prefix for WebSocket destinations
    private static final String TOPIC_PREFIX = "/topic";
    
    /**
     * Constructor that accepts a messaging template if available.
     * 
     * @param messagingTemplate The Spring messaging template
     */
    @Autowired
    public WebSocketMessagingDelegate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        log.info("WebSocket messaging delegate initialized");
    }
    
    /**
     * Send a scan status notification to all subscribers for a specific job
     * 
     * @param notification The notification object to send
     * @param jobId The ID of the scan job
     */
    public void sendScanStatusUpdate(ScanStatusNotification notification, String jobId) {
        try {
            // Send directly using SimpMessagingTemplate
            String jobDestination = TOPIC_PREFIX + "/scan-status/" + jobId;
            messagingTemplate.convertAndSend(jobDestination, notification);
            
            // Also send to general scan status topic
            String generalDestination = TOPIC_PREFIX + "/scan-status";
            messagingTemplate.convertAndSend(generalDestination, notification);
            
            log.debug("Sent notification to job channel and general channel");        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }
      /**
     * Send a job-specific notification with data payload
     * 
     * @param jobId The job ID to send the notification for
     * @param notificationType The type of notification (e.g., "progress", "quasi-identifiers", "results")
     * @param data The notification data payload
     */
    public void sendJobNotification(String jobId, String notificationType, Object data) {
        try {
            // Send to scan-specific topic with notification type (matching client subscription pattern)
            String jobDestination = TOPIC_PREFIX + "/scans/" + jobId + "/" + notificationType;
            messagingTemplate.convertAndSend(jobDestination, data);
            
            // Also send to general scan notification topic for monitoring
            String generalDestination = TOPIC_PREFIX + "/scan-notifications/" + notificationType;
            messagingTemplate.convertAndSend(generalDestination, data);
            
            log.debug("Sent {} notification for job {} to specific and general channels", notificationType, jobId);        } catch (Exception e) {
            log.warn("Failed to send {} notification for job {}: {}", notificationType, jobId, e.getMessage());
        }
    }
    
    /**
     * Send a direct message to a specific user
     * 
     * @param username The username of the recipient
     * @param message The message to send
     */
    public void sendDirectMessage(String username, Object message) {
        try {
            // Send directly to user's private queue
            messagingTemplate.convertAndSendToUser(username, "/queue/messages", message);
            log.debug("Sent direct message to user: {}", username);
        } catch (Exception e) {
            log.warn("Failed to send direct message to user {}: {}", username, e.getMessage());
        }
    }
    
    /**
     * Send a message to a WebSocket destination
     */
    private void sendMessage(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage());
        }
    }
}