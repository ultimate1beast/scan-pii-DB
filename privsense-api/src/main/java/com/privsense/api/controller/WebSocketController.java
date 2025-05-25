package com.privsense.api.controller;

import com.privsense.api.dto.notification.ClientNotification;
import com.privsense.api.dto.notification.ServerResponse;
import com.privsense.api.dto.websocket.SubscriptionRequest;
import com.privsense.api.dto.websocket.ScanStatusMessage;
import com.privsense.core.service.NotificationService;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.api.service.WebSocketMessagingDelegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.security.Principal;
import java.util.UUID;

/**
 * Controller to handle incoming WebSocket messages from clients.
 * This creates bidirectional communication, complementing the server-to-client
 * updates sent by WebSocketMessagingDelegate.
 */
@Slf4j
@Controller
@ConditionalOnProperty(name = "privsense.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketController {
    
    private final WebSocketMessagingDelegate messagingDelegate;
    private final ScanOrchestrationService scanService;
    private final NotificationService notificationService;
    
    @Autowired
    public WebSocketController(WebSocketMessagingDelegate messagingDelegate,
                              ScanOrchestrationService scanService,
                              NotificationService notificationService) {
        this.messagingDelegate = messagingDelegate;
        this.scanService = scanService;
        this.notificationService = notificationService;
        log.info("WebSocket controller initialized");
    }
    
    /**
     * Handles client notifications sent to /app/notify
     * 
     * @param notification The notification from the client
     * @return A response to send back to all subscribers of /topic/notifications
     */
    @MessageMapping("/notify")
    @SendTo("/topic/notifications")
    public ServerResponse handleNotification(ClientNotification notification) {
        log.info("Received client notification: {}", notification);
        
        // Process the notification
        ServerResponse response = new ServerResponse();
        response.setStatus("received");
        response.setMessage("Notification processed successfully");
        response.setTimestamp(System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * Handles subscription requests for specific scan jobs
     * 
     * @param jobId The ID of the job to subscribe to
     * @return A response sent only to the requesting user
     */
    @MessageMapping("/subscribe/{jobId}")
    @SendToUser("/queue/responses")
    public ServerResponse handleSubscription(@DestinationVariable String jobId) {
        log.info("Client subscribed to job updates: {}", jobId);
        
        ServerResponse response = new ServerResponse();
        response.setStatus("subscribed");
        response.setMessage("Subscribed to updates for job: " + jobId);
        response.setTimestamp(System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * Handles detailed subscription requests for specific scan updates.
     * Client sends this message to subscribe to a specific scan's updates.
     * 
     * @param request Subscription details
     * @param headerAccessor Message headers with session info
     * @param principal Authenticated user
     */
    @MessageMapping("/subscribe-scan")
    @SendToUser("/queue/responses")
    public ServerResponse subscribeToScan(SubscriptionRequest request, 
                                         SimpMessageHeaderAccessor headerAccessor,
                                         Principal principal) {
        if (request == null || request.getScanId() == null) {
            log.warn("Invalid subscription request received");
            ServerResponse errorResponse = new ServerResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Invalid subscription request");
            errorResponse.setTimestamp(System.currentTimeMillis());
            return errorResponse;
        }
        
        try {
            UUID scanId = UUID.fromString(request.getScanId());
            String sessionId = headerAccessor.getSessionId();
            String username = principal.getName();
            
            log.info("User {} subscribed to scan {} updates from session {}", 
                    username, scanId, sessionId);
            
            // Send initial status immediately upon subscription
            sendInitialStatus(scanId, username);
            
            ServerResponse response = new ServerResponse();
            response.setStatus("subscribed");
            response.setMessage("Subscribed to updates for scan: " + scanId);
            response.setTimestamp(System.currentTimeMillis());
            return response;
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid scan ID in subscription request: {}", request.getScanId());
            ServerResponse errorResponse = new ServerResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Invalid scan ID format");
            errorResponse.setTimestamp(System.currentTimeMillis());
            return errorResponse;
        } catch (Exception e) {
            log.error("Error processing subscription request", e);
            ServerResponse errorResponse = new ServerResponse();
            errorResponse.setStatus("error");
            errorResponse.setMessage("Internal server error");
            errorResponse.setTimestamp(System.currentTimeMillis());
            return errorResponse;
        }
    }
    
    /**
     * Sends initial scan status to user upon subscription
     */
    private void sendInitialStatus(UUID scanId, String username) {
        try {
            // Get current scan status using ScanOrchestrationService
            try {
                // Get current job status directly instead of using findScanById
                var scanJobResponse = scanService.getJobStatus(scanId);
                notificationService.sendDirectMessage(username, new ScanStatusMessage(scanJobResponse));
            } catch (IllegalArgumentException e) {
                log.warn("Scan not found for ID: {}", scanId);
                // Send a "not found" status message instead of failing silently
                ServerResponse notFoundResponse = new ServerResponse();
                notFoundResponse.setStatus("not_found");
                notFoundResponse.setMessage("Scan not found with ID: " + scanId);
                notFoundResponse.setTimestamp(System.currentTimeMillis());
                notificationService.sendDirectMessage(username, notFoundResponse);
            }
        } catch (Exception e) {
            log.error("Failed to send initial scan status for scan {}", scanId, e);
        }
    }
}