package com.privsense.api.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Response DTO containing system information.
 */
@Data
public class SystemInfoResponse {
    
    /**
     * The version of the PrivSense application.
     */
    private String version;
    
    /**
     * The build timestamp.
     */
    private String buildTime;
    
    /**
     * System uptime in human-readable format.
     */
    private String uptime;
    
    /**
     * The current environment (dev, test, prod).
     */
    private String environment;
    
    /**
     * Available features in the current system.
     */
    private Map<String, Boolean> features = new HashMap<>();
    
    /**
     * Any additional information.
     */
    private Map<String, String> additionalInfo = new HashMap<>();
}