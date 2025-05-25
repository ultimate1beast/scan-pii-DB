package com.privsense.api.dto;

import lombok.Data;
import java.util.Map;

/**
 * Response DTO for API health information.
 */
@Data
public class ApiHealthResponse {
    /**
     * Overall status of the API (UP, DOWN, DEGRADED).
     */
    private String status;
    
    /**
     * ISO-8601 timestamp of when the health check was performed.
     */
    private String timestamp;
    
    /**
     * Status of individual components within the system.
     */
    private Map<String, Object> components;
}