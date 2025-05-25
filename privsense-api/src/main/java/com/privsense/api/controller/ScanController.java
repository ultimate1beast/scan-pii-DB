package com.privsense.api.controller;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.PageResponse;
import com.privsense.api.dto.ScanStatsDTO;
import com.privsense.api.dto.ScannedTableDTO;
import com.privsense.api.dto.ScannedColumnDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.api.util.LinkBuilder;
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
import org.springframework.security.access.prepost.PreAuthorize;
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
    
    // Constants for HTTP headers to avoid duplication
    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String ATTACHMENT_FILENAME_PREFIX = "attachment; filename=\"privsense_scan_report_";

    /**
     * Initiates a new database scan operation.
     * Only administrators can start new scans.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Start a new scan",
        description = "Initiates a new database scan to detect PII in the database (Admin only)"
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
        
        // Explicitly mark the response as successful even though the job is just starting
        jobResponse.addMeta("status", "SUCCESS");
        
        // Add HATEOAS links to the response
        LinkBuilder.addScanLinks(jobResponse, jobId);
        
        return ResponseEntity
                .created(URI.create("/api/v1/scans/" + jobId))
                .body(jobResponse);
    }

    /**
     * Lists all scans in the system with pagination support.
     * ADMIN users can see all scans, API_USER can only view scans they initiated.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
    @Operation(
        summary = "List all scans",
        description = "Returns a paginated list of all scan jobs in the system (Admins see all, API users see only their scans)"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "List of scan jobs retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = PageResponse.class))
    )
    public ResponseEntity<PageResponse<ScanJobResponse>> getAllScans(
            @Parameter(description = "Page number (0-based)", schema = @Schema(defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", schema = @Schema(defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by scan status (completed, pending, failed)")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by database connection ID")
            @RequestParam(required = false) UUID connectionId) {
        
        log.debug("Getting scans with page={}, size={}, status={}, connectionId={}", 
                page, size, status, connectionId);
                
        PageResponse<ScanJobResponse> pageResponse = scanService.getPagedJobs(page, size, status, connectionId);
        
        // Set success flag for each individual scan job in the list
        if (pageResponse.getContent() != null) {
            for (ScanJobResponse scanJob : pageResponse.getContent()) {
                scanJob.addMeta("status", "SUCCESS");
                // Add HATEOAS links to each scan in the collection
                LinkBuilder.addScanLinks(scanJob, scanJob.getJobId());
            }
        }
        
        // Add pagination links to the response
        LinkBuilder.addPaginationLinks(pageResponse, page, size, pageResponse.getTotalPages());
        
        return ResponseEntity.ok(pageResponse);
    }

    /**
     * Gets the current status of a scan job.
     * Both roles can access this but API_USER can only see their own scans.
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
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
            
            // Explicitly mark the response as successful regardless of scan status
            // This ensures the HTTP call itself is marked as successful
            response.addMeta("status", "SUCCESS");
            
            // Add HATEOAS links to the response
            LinkBuilder.addScanLinks(response, jobId);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
    }

    /**
     * Exports the scan report in various formats.
     * Both roles can access, but API_USER can only export their own scans.
     */
    @GetMapping(value = "/{jobId}/report")
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
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
    public ResponseEntity<Object> exportScanReport(
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
            String filenameExtension;
            ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
            
            switch (requestedFormat) {
                case "json":
                    // Use a DTO to avoid LazyInitializationException
                    ComplianceReportDTO reportDTO = scanService.getScanReportAsDTO(jobId);
                    return responseBuilder
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(reportDTO);
                    
                case "pdf":
                    byte[] pdfContent = scanService.exportReportAsPdf(jobId);
                    filenameExtension = ".pdf";
                    responseBuilder
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HEADER_CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + jobId + filenameExtension + "\"")
                            .header(HEADER_CONTENT_LENGTH, String.valueOf(pdfContent.length));
                    return responseBuilder.body(pdfContent);
                    
                case "text":
                    byte[] textContent = scanService.exportReportAsText(jobId);
                    filenameExtension = ".txt";
                    responseBuilder
                            .contentType(MediaType.TEXT_PLAIN)
                            .header(HEADER_CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + jobId + filenameExtension + "\"")
                            .header(HEADER_CONTENT_LENGTH, String.valueOf(textContent.length));
                    return responseBuilder.body(textContent);
                    
                case "csv":
                    byte[] csvContent = scanService.exportReportAsCsv(jobId);
                    filenameExtension = ".csv";
                    responseBuilder
                            .contentType(MediaType.parseMediaType("text/csv"))
                            .header(HEADER_CONTENT_DISPOSITION, ATTACHMENT_FILENAME_PREFIX + jobId + filenameExtension + "\"")
                            .header(HEADER_CONTENT_LENGTH, String.valueOf(csvContent.length));
                    return responseBuilder.body(csvContent);
                    
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
     * Only administrators can cancel scans.
     */
    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cancel scan",
        description = "Cancels an in-progress scan job (Admin only)"
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

    /**
     * Retrieves the PII detection results for a scan with filtering options.
     * Both roles can access, but API_USER can only see their own scan results.
     */
    @GetMapping("/{jobId}/results")
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
    @Operation(
        summary = "Get scan results with filtering",
        description = "Returns PII detection results with filtering by PII type and confidence score"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Results retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    @ApiResponse(responseCode = "409", description = "Scan not completed yet")
    public ResponseEntity<List<DetectionResultDTO>> getScanResults(
            @Parameter(description = "ID of the scan job", required = true)
            @PathVariable UUID jobId,
            @Parameter(description = "Filter by PII type (e.g., EMAIL, NAME, SSN)")
            @RequestParam(required = false) String piiType,
            @Parameter(description = "Minimum confidence score threshold (0.0-1.0)")
            @RequestParam(required = false) Double confidenceMin,
            @Parameter(description = "Page number (0-based)", schema = @Schema(defaultValue = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", schema = @Schema(defaultValue = "20"))
            @RequestParam(defaultValue = "20") int size) {
        log.debug("Getting filtered scan results for job {}: piiType={}, confidenceMin={}, page={}, size={}", 
                jobId, piiType, confidenceMin, page, size);

        // Check if the scan exists and is completed
        try {
            ScanJobResponse jobStatus = scanService.getJobStatus(jobId);
            if (!jobStatus.isCompleted()) {
                log.warn("Results requested for incomplete scan {}, status is {}", 
                        jobId, jobStatus.getStatus());
                throw new ConflictException("Scan results not available: scan is not completed yet");
            }

            List<DetectionResultDTO> results = scanService.getScanResultsWithFiltering(
                    jobId, piiType, confidenceMin, page, size);
            
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
    }

    /**
     * Retrieves statistics about scan results.
     * Both roles can access, but API_USER can only see stats for their own scans.
     */
    @GetMapping("/{jobId}/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
    @Operation(
        summary = "Get scan statistics",
        description = "Returns statistics about the scan results including PII distribution"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Statistics retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanStatsDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    public ResponseEntity<ScanStatsDTO> getScanStatistics(
            @Parameter(description = "ID of the scan job", required = true)
            @PathVariable UUID jobId) {
        log.debug("Getting scan statistics for job {}", jobId);
        
        try {
            ScanStatsDTO stats = scanService.getScanStatistics(jobId);
            
            // Add HATEOAS links to the statistics
            stats.addLink("self", "/api/v1/scans/" + jobId + "/stats");
            stats.addLink("scan", "/api/v1/scans/" + jobId);
            stats.addLink("tables", "/api/v1/scans/" + jobId + "/tables");
            stats.addLink("results", "/api/v1/scans/" + jobId + "/results");
            stats.addLink("report", "/api/v1/scans/" + jobId + "/report");
            
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
    }

    /**
     * Lists tables that were scanned during a scan job.
     * Both roles can access, but API_USER can only see their own scan details.
     */
    @GetMapping("/{jobId}/tables")
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
    @Operation(
        summary = "Get scanned tables",
        description = "Returns a list of tables that were scanned with PII statistics"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Scanned tables retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(responseCode = "404", description = "Scan job not found")
    public ResponseEntity<List<ScannedTableDTO>> getScannedTables(
            @Parameter(description = "ID of the scan job", required = true)
            @PathVariable UUID jobId) {
        log.debug("Getting scanned tables for job {}", jobId);
        
        try {
            List<ScannedTableDTO> tables = scanService.getScannedTables(jobId);
            
            // Add HATEOAS links to each table
            for (ScannedTableDTO table : tables) {
                table.addLink("self", "/api/v1/scans/" + jobId + "/tables/" + table.getTableName());
                table.addLink("columns", "/api/v1/scans/" + jobId + "/tables/" + table.getTableName() + "/columns");
                table.addLink("scan", "/api/v1/scans/" + jobId);
            }
            
            return ResponseEntity.ok(tables);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        }
    }

    /**
     * Lists columns from a specific table that were scanned during a scan job.
     * Both roles can access, but API_USER can only see their own scan details.
     */
    @GetMapping("/{jobId}/tables/{tableName}/columns")
    @PreAuthorize("hasAnyRole('ADMIN', 'API_USER')")
    @Operation(
        summary = "Get columns from scanned table",
        description = "Returns columns from a specific table with PII detection results"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Table columns retrieved successfully",
        content = @Content(mediaType = "application/json")
    )
    @ApiResponse(responseCode = "404", description = "Scan job or table not found")
    public ResponseEntity<List<ScannedColumnDTO>> getScannedColumns(
            @Parameter(description = "ID of the scan job", required = true)
            @PathVariable UUID jobId,
            @Parameter(description = "Name of the table", required = true)
            @PathVariable String tableName) {
        log.debug("Getting scanned columns for job {} and table {}", jobId, tableName);
        
        try {
            List<ScannedColumnDTO> columns = scanService.getScannedColumns(jobId, tableName);
            
            // Add HATEOAS links to each column
            for (ScannedColumnDTO column : columns) {
                column.addLink("self", "/api/v1/scans/" + jobId + "/tables/" + tableName + "/columns/" + column.getColumnName());
                column.addLink("table", "/api/v1/scans/" + jobId + "/tables/" + tableName);
                column.addLink("scan", "/api/v1/scans/" + jobId);
            }
            
            return ResponseEntity.ok(columns);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Scan job not found: " + jobId);
        } catch (ResourceNotFoundException e) {
            throw e;
        }
    }
}