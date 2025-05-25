package com.privsense.api.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Response DTO containing health status information for the system.
 */
@Data
public class SystemHealthResponse {
    
    /**
     * Overall status of the system.
     * Values: UP, DOWN, DEGRADED
     */
    private String status;
    
    /**
     * Detailed status of each component.
     * Key: component name (e.g., "api", "database", "ner-service")
     * Value: UP, DOWN, DEGRADED
     */
    private Map<String, String> components = new HashMap<>();
    
    /**
     * Timestamp of when the health check was performed.
     */
    private String timestamp;
    
    /**
     * Any additional details or error messages.
     */
    private String details;
    
    /**
     * Default constructor.
     */
    public SystemHealthResponse() {
        this.timestamp = java.time.OffsetDateTime.now().toString();
    }
    
    /**
     * Add a component's health status.
     * 
     * @param componentName The name of the component
     * @param status The status of the component
     */
    public void addComponent(String componentName, String status) {
        components.put(componentName, status);
    }
}