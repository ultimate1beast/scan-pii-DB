package com.privsense.api.controller;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.service.ScanOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.List;
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
    @ApiResponse(
        responseCode = "201", 
        description = "Scan job created",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanJobResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "404", description = "Database connection not found")
    public ResponseEntity<ScanJobResponse> startScan(
            @Valid @RequestBody 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Scan configuration parameters", 
                required = true, 
                content = @Content(schema = @Schema(implementation = ScanRequest.class))
            ) ScanRequest request) {
        
        UUID jobId = scanService.submitScanJob(request);
        // Get the job status to return in the response
        ScanJobResponse jobResponse = scanService.getJobStatus(jobId);
        return ResponseEntity
                .created(URI.create("/api/v1/scans/" + jobId))
                .body(jobResponse);
    }

    /**
     * Lists all scans in the system.
     */
    @GetMapping
    @Operation(
        summary = "List all scans",
        description = "Returns a list of all scan jobs in the system"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "List of scan jobs retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanJobResponse.class))
    )
    public ResponseEntity<List<ScanJobResponse>> getAllScans() {
        List<ScanJobResponse> scans = scanService.getAllJobs();
        return ResponseEntity.ok(scans);
    }

    /**
     * Gets the current status of a scan job.
     */
    @GetMapping("/{jobId}")
    @Operation(
        summary = "Get scan status",
        description = "Returns the current status of a running scan job"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Scan status retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanJobResponse.class))
    )
    @ApiResponse(
        responseCode = "404", 
        description = "Scan job not found"
    )
    public ResponseEntity<ScanJobResponse> getScanStatus(
            @Parameter(description = "ID of the scan job", required = true)
            @PathVariable UUID jobId) {
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
    @ApiResponse(
        responseCode = "200", 
        description = "Report exported successfully",
        content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ComplianceReportDTO.class)),
            @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")),
            @Content(mediaType = "text/plain", schema = @Schema(type = "string")),
            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))
        }
    )
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    @ApiResponse(responseCode = "409", description = "Scan not completed yet")
    @ApiResponse(responseCode = "415", description = "Requested format not supported")
    public ResponseEntity<?> exportScanReport(
            @Parameter(description = "ID of the scan job", required = true)
            @PathVariable UUID jobId,
            @Parameter(description = "Export format (json, csv, text, pdf)", schema = @Schema(allowableValues = {"json", "csv", "text", "pdf"}))
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
        
        // Always prioritize the format parameter if it's provided
        String requestedFormat = "csv"; // Default format
        if (format != null) {
            requestedFormat = format.toLowerCase();
        } else {
            // Only use Accept header if format parameter is not provided
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null) {
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
        }
        
        log.debug("Processing report request for job {} in format {}", jobId, requestedFormat);
        
        // Handle each format type
        try {
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
                    byte[] csvContent = scanService.exportReportAsCsv(jobId);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType("text/csv"))
                            .header("Content-Disposition", "attachment; filename=\"privsense_scan_report_" + jobId + ".csv\"")
                            .header("Content-Length", String.valueOf(csvContent.length))
                            .body(csvContent);
                    
                default:
                    throw new IllegalArgumentException("Unsupported format: " + requestedFormat);
            }
        } catch (Exception e) {
            log.error("Error exporting report in {} format: {}", requestedFormat, e.getMessage(), e);
            throw e;
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
    public ResponseEntity<Void> cancelScan(
            @Parameter(description = "ID of the scan job to cancel", required = true)
            @PathVariable UUID jobId) {
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