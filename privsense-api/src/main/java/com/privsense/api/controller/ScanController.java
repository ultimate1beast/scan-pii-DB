package com.privsense.api.controller;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.service.ScanOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public ResponseEntity<ScanJobResponse> startScan(@Valid @RequestBody ScanRequest request) {
        UUID jobId = scanService.submitScanJob(request);
        // Get the job status to return in the response
        ScanJobResponse jobResponse = scanService.getJobStatus(jobId);
        return ResponseEntity
                .created(URI.create("/api/v1/scans/" + jobId))
                .body(jobResponse);
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
            log.debug("Scan status for job {}: status={}, completed={}", 
                    jobId, response.getStatus(), response.isCompleted());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
    }

    /**
     * Exports the scan report in various formats.
     */
    @GetMapping(value = "/{jobId}/report")
    @Operation(
        summary = "Export scan report in specific format",
        description = "Exports the scan report in various formats (CSV, text, PDF, JSON)"
    )
    @ApiResponse(responseCode = "200", description = "Report exported successfully")
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    @ApiResponse(responseCode = "409", description = "Scan not completed yet")
    @ApiResponse(responseCode = "415", description = "Requested format not supported")
    public ResponseEntity<?> exportScanReport(
            @PathVariable UUID jobId,
            @RequestParam(required = false) String format,
            HttpServletRequest request) {
        
        // Check if the scan exists and is completed
        try {
            ScanJobResponse jobStatus = scanService.getJobStatus(jobId);
            log.debug("Report request for job {}: status={}, completed={}", 
                    jobId, jobStatus.getStatus(), jobStatus.isCompleted());
            
            if (!jobStatus.isCompleted()) {
                log.warn("Report requested for incomplete scan {}, status is {}", 
                        jobId, jobStatus.getStatus());
                throw new ConflictException("Scan report not available: scan is not completed yet");
            }
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
        
        // Get the Accept header for format determination
        String acceptHeader = request.getHeader("Accept");
        String requestedFormat = format != null ? format.toLowerCase() : "csv"; // Default format
        
        // If format parameter is not provided, try to determine from Accept header
        if (format == null && acceptHeader != null) {
            acceptHeader = acceptHeader.toLowerCase();
            if (acceptHeader.contains("json")) {
                requestedFormat = "json";
            } else if (acceptHeader.contains("pdf")) {
                requestedFormat = "pdf";
            } else if (acceptHeader.contains("plain")) {
                requestedFormat = "text";
            } else if (acceptHeader.contains("csv")) {
                requestedFormat = "csv";
            }
        }
        
        log.debug("Processing report request for job {} in format {}", jobId, requestedFormat);
        
        // Handle each format type
        switch (requestedFormat) {
            case "json":
                // Use a DTO to avoid LazyInitializationException
                ComplianceReportDTO reportDTO = scanService.getScanReportAsDTO(jobId);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(reportDTO);
                
            case "pdf":
                byte[] pdfContent = scanService.exportReportAsPdf(jobId);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header("Content-Disposition", "attachment; filename=\"privsense_scan_report_" + jobId + ".pdf\"")
                        .header("Content-Length", String.valueOf(pdfContent.length))
                        .body(pdfContent);
                
            case "text":
                byte[] textContent = scanService.exportReportAsText(jobId);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .header("Content-Disposition", "attachment; filename=\"privsense_scan_report_" + jobId + ".txt\"")
                        .header("Content-Length", String.valueOf(textContent.length))
                        .body(textContent);
                
            case "csv":
            default:
                byte[] csvContent = scanService.exportReportAsCsv(jobId);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .header("Content-Disposition", "attachment; filename=\"privsense_scan_report_" + jobId + ".csv\"")
                        .header("Content-Length", String.valueOf(csvContent.length))
                        .body(csvContent);
        }
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