package com.privsense.api.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a notification sent from a client to the server via WebSocket.
 * Used in bidirectional WebSocket communication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientNotification {
    
    /**
     * Type of notification (e.g., "info", "request", "action")
     */
    private String type;
    
    /**
     * The message content
     */
    private String message;
    
    /**
     * Any additional context or data payload as a JSON string
     */
    private String payload;
    
    /**
     * Client-generated timestamp
     */
    private Long timestamp;
}