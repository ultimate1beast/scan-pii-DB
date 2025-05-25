package com.privsense.api.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a response sent from the server to clients via WebSocket.
 * Used for bidirectional communication and to acknowledge client requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerResponse {
    
    /**
     * Status of the response (e.g., "success", "error", "received")
     */
    private String status;
    
    /**
     * The message content
     */
    private String message;
    
    /**
     * Any additional data as a JSON string
     */
    private String data;
    
    /**
     * Server-generated timestamp
     */
    private Long timestamp;
}