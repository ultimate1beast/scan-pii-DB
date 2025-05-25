package com.privsense.core.service;

/**
 * Service interface for sending notifications across the system.
 * This interface defines the contract for notification delivery mechanisms.
 */
public interface NotificationService {

    /**
     * Send a scan status update notification.
     * 
     * @param scanJobResponseObj The scan job response object containing status information
     */
    void sendScanStatusUpdate(Object scanJobResponseObj);

    /**
     * Send a job-specific notification with data payload.
     *
     * @param jobId The job ID to send the notification for
     * @param notificationType The type of notification (e.g., "progress", "quasi-identifiers", "results")
     * @param data The notification data payload
     */
    void sendJobNotification(String jobId, String notificationType, Object data);

    /**
     * Send a direct message to a specific WebSocket destination.
     * 
     * @param destination The WebSocket destination
     * @param message The message to send
     */
    void sendDirectMessage(String destination, Object message);
}