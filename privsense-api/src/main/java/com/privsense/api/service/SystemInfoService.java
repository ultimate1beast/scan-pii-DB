package com.privsense.api.service;

import com.privsense.api.dto.SystemHealthResponse;
import com.privsense.api.dto.SystemInfoResponse;

/**
 * Service interface for retrieving system information and health status.
 */
public interface SystemInfoService {
    
    /**
     * Gets the current health status of the system.
     *
     * @return A SystemHealthResponse containing health information
     */
    SystemHealthResponse getHealthStatus();
    
    /**
     * Gets system information including version, environment, and available features.
     *
     * @return A SystemInfoResponse containing system information
     */
    SystemInfoResponse getSystemInfo();
}