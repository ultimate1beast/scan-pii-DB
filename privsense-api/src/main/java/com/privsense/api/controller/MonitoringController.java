package com.privsense.api.controller;

import com.privsense.api.service.RequestMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring API performance and usage metrics.
 * Provides endpoints for frontend applications to observe API behavior.
 */
@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Monitoring", description = "APIs for monitoring system performance metrics")
public class MonitoringController {

    private final RequestMetricsService requestMetricsService;

    /**
     * Gets basic performance metrics for the API.
     * This provides frontend applications with insight into API performance.
     * 
     * @return Performance metrics information
     */
    @GetMapping("/metrics")
    @Operation(
        summary = "Get API performance metrics",
        description = "Returns basic performance metrics for the API, including memory usage and thread counts"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Metrics retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, Object>> getMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Runtime metrics
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // Convert to MB
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;
        
        metrics.put("memory", Map.of(
            "max", maxMemory + " MB",
            "total", totalMemory + " MB",
            "free", freeMemory + " MB",
            "used", usedMemory + " MB",
            "usagePercentage", Math.round((double) usedMemory / totalMemory * 100) + "%"
        ));
        
        // Thread metrics
        int activeThreadCount = Thread.activeCount();
        metrics.put("threads", Map.of(
            "active", activeThreadCount
        ));
        
        // System metrics
        metrics.put("cpu", Map.of(
            "availableProcessors", runtime.availableProcessors()
        ));
        
        log.debug("Performance metrics requested");
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Gets usage statistics for the API.
     * This provides frontend applications with insight into API usage patterns.
     * 
     * @return API usage statistics
     */
    @GetMapping("/usage")
    @Operation(
        summary = "Get API usage statistics",
        description = "Returns usage statistics for the API, including request counts and error rates"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Usage statistics retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    public ResponseEntity<Map<String, Object>> getUsageStats() {
        Map<String, Object> usageStats = new HashMap<>();
        
        // Get real request statistics from the metrics service
        Map<String, Object> requests = new HashMap<>();
        requests.put("total", requestMetricsService.getTotalRequests());
        requests.put("lastMinute", requestMetricsService.getRequestsLastMinute());
        
        // Get endpoint data - convert to Map<String, Object> for consistency
        Map<String, Object> endpoints = new HashMap<>();
        requestMetricsService.getRequestsByEndpoint().forEach((endpoint, count) -> {
            endpoints.put(endpoint, count);
        });
        
        requests.put("byEndpoint", endpoints);
        
        // Get error statistics
        Map<String, Object> errors = new HashMap<>();
        errors.put("total", requestMetricsService.getTotalErrors());
        errors.put("rate", requestMetricsService.getErrorRate());
        
        // Get error status code data - convert to Map<String, Object> for consistency
        Map<String, Object> byStatus = new HashMap<>();
        requestMetricsService.getErrorsByStatus().forEach((statusCode, count) -> {
            byStatus.put(statusCode.toString(), count);
        });
        
        errors.put("byStatus", byStatus);
        
        usageStats.put("requests", requests);
        usageStats.put("errors", errors);
        
        log.debug("API usage statistics requested");
        return ResponseEntity.ok(usageStats);
    }
    
    /**
     * Resets all usage statistics.
     * This allows administrators to clear metrics when needed.
     * 
     * @return A confirmation message
     */
    @DeleteMapping("/usage")
    @Operation(
        summary = "Reset API usage statistics",
        description = "Clears all API usage statistics. Requires administrative privileges."
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Usage statistics cleared successfully",
        content = @Content(mediaType = "application/json")
    )
    @SecurityRequirement(name = "basicAuth")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> resetUsageStats() {
        requestMetricsService.resetMetrics();
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "All API usage statistics have been reset");
        
        log.info("API usage statistics have been reset");
        return ResponseEntity.ok(response);
    }
}