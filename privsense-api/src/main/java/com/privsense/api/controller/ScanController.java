package com.privsense.api.controller;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.core.model.ComplianceReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller for managing PII scan operations.
 */
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor
@Tag(name = "PII Scans", description = "APIs for managing database PII scans")
public class ScanController {

    private final ScanOrchestrationService scanService;

    /**
     * Initiates a new database scan operation.
     */
    @PostMapping
    @Operation(
        summary = "Start a new scan",
        description = "Initiates a new database scan to detect PII in the database"
    )
    @ApiResponse(responseCode = "201", description = "Scan job created")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "404", description = "Database connection not found")
    public ResponseEntity<Void> startScan(@Valid @RequestBody ScanRequest request) {
        UUID jobId = scanService.submitScanJob(request);
        return ResponseEntity
                .created(URI.create("/api/v1/scans/" + jobId))
                .build();
    }

    /**
     * Gets the current status of a scan job.
     */
    @GetMapping("/{jobId}")
    @Operation(
        summary = "Get scan status",
        description = "Returns the current status of a running scan job"
    )
    @ApiResponse(responseCode = "200", description = "Scan status retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    public ResponseEntity<ScanJobResponse> getScanStatus(@PathVariable UUID jobId) {
        try {
            // Using straight service method that returns DTO
            ScanJobResponse response = scanService.getJobStatus(jobId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
    }

    /**
     * Gets the report for a completed scan.
     */
    @GetMapping(value = "/{jobId}/report", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get scan report",
        description = "Returns the full scan report with PII findings for a completed scan"
    )
    @ApiResponse(responseCode = "200", description = "Report retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    @ApiResponse(responseCode = "409", description = "Scan not completed yet")
    public ResponseEntity<ComplianceReport> getScanReport(@PathVariable UUID jobId) {
        ComplianceReport report = scanService.getScanReport(jobId);
        return ResponseEntity.ok(report);
    }

    /**
     * Exports the scan report in various formats.
     */
    @GetMapping(value = "/{jobId}/report", produces = {
            "application/csv", 
            "text/plain", 
            "application/pdf"
    })
    @Operation(
        summary = "Export scan report in specific format",
        description = "Exports the scan report in various formats (CSV, text, PDF)"
    )
    public ResponseEntity<byte[]> exportScanReport(
            @PathVariable UUID jobId,
            @RequestHeader("Accept") String format) {
        
        // This would be implemented based on the specific format requested
        // For now, we'll return a simple placeholder response
        String mockReport = "Mock report for scan job: " + jobId;
        return ResponseEntity.ok(mockReport.getBytes());
    }

    /**
     * Cancels an in-progress scan job.
     */
    @DeleteMapping("/{jobId}")
    @Operation(
        summary = "Cancel scan",
        description = "Cancels an in-progress scan job"
    )
    @ApiResponse(responseCode = "204", description = "Scan cancelled successfully")
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    @ApiResponse(responseCode = "409", description = "Scan already completed or failed")
    public ResponseEntity<Void> cancelScan(@PathVariable UUID jobId) {
        try {
            scanService.cancelScan(jobId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        } catch (IllegalStateException e) {
            throw new ConflictException(e.getMessage());
        }
    }
}