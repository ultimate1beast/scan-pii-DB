package com.privsense.api.controller;

import com.privsense.api.dto.SystemHealthResponse;
import com.privsense.api.dto.SystemInfoResponse;
import com.privsense.api.service.SystemInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system-level operations and monitoring.
 * Provides endpoints for health checks, version info, and system status.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "System", description = "APIs for system operations and monitoring")
public class SystemController {

    private final SystemInfoService systemInfoService;

    /**
     * Gets the health status of the system.
     * This endpoint allows clients to check if the API is functioning correctly.
     * 
     * @return Health status information
     */
    @GetMapping("/health")
    @Operation(
        summary = "Get system health",
        description = "Returns the health status of the system, including API, database, and NER service status"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Health check successful",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemHealthResponse.class))
    )
    public ResponseEntity<SystemHealthResponse> getHealth() {
        SystemHealthResponse healthResponse = systemInfoService.getHealthStatus();
        log.debug("Health check requested, status: {}", healthResponse.getStatus());
        return ResponseEntity.ok(healthResponse);
    }

    /**
     * Gets information about the system, including version, uptime, and environment.
     * 
     * @return System information
     */
    @GetMapping("/info")
    @Operation(
        summary = "Get system information",
        description = "Returns information about the system, including version, uptime, and environment"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "System information retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = SystemInfoResponse.class))
    )
    public ResponseEntity<SystemInfoResponse> getSystemInfo() {
        SystemInfoResponse infoResponse = systemInfoService.getSystemInfo();
        log.debug("System info requested, version: {}", infoResponse.getVersion());
        return ResponseEntity.ok(infoResponse);
    }
}