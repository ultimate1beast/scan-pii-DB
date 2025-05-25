package com.privsense.api.service;

import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.PageResponse;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.ScanStatsDTO;
import com.privsense.api.dto.ScannedTableDTO;
import com.privsense.api.dto.ScannedColumnDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.api.mapper.EntityMapper;
import com.privsense.api.mapper.DetectionMapper;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.ScanMetadata;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.TableInfo;
import com.privsense.core.service.*;
import com.privsense.core.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;

/**
 * Service that orchestrates scanning operations and report exports.
 * This class follows the Facade pattern to provide a simplified interface to the scan subsystem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanOrchestrationService {

    private final ScanJobManagementService scanJobManagementService;
    private final ScanReportService scanReportService;
    private final EnhancedReportExportService enhancedReportExportService;
    private final ScanPersistenceService scanPersistenceService;
    private final NotificationService notificationService;
    private final EntityMapper entityMapper;

    /**
     * Submits a new scan job.
     *
     * @param scanRequest The scan request parameters
     * @return The ID of the created job
     */
    public UUID submitScanJob(ScanRequest scanRequest) {
        UUID jobId = scanJobManagementService.submitScanJob(scanRequest);
        
        // Send WebSocket notification about new scan
        ScanJobResponse jobStatus = getJobStatus(jobId);
        notificationService.sendScanStatusUpdate(jobStatus);
        
        return jobId;
    }

    /**
     * Gets the status of a scan job.
     *
     * @param jobId The job ID
     * @return The scan job response with status information
     */
    public ScanJobResponse getJobStatus(UUID jobId) {
        return (ScanJobResponse) scanJobManagementService.getJobStatus(jobId);
    }

    /**
     * Cancels an in-progress scan job.
     *
     * @param jobId The job ID
     */
    public void cancelScan(UUID jobId) {
        scanJobManagementService.cancelScan(jobId);
        
        // Send WebSocket notification about cancelled scan
        ScanJobResponse jobStatus = getJobStatus(jobId);
        notificationService.sendScanStatusUpdate(jobStatus);
    }

    /**
     * Gets all scan jobs in the system.
     *
     * @return A list of all scan jobs
     */
    public List<ScanJobResponse> getAllJobs() {
        List<Object> jobList = scanJobManagementService.getAllJobs();
        return jobList.stream()
                .map(job -> (ScanJobResponse) job)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets paginated and filtered scan jobs.
     *
     * @param page Page number (0-based)
     * @param size Items per page
     * @param status Optional status filter (completed, pending, failed)
     * @param connectionId Optional connection ID filter
     * @return A paginated response containing scan job information
     */
    public PageResponse<ScanJobResponse> getPagedJobs(int page, int size, String status, UUID connectionId) {
        List<ScanMetadata> scanList;
        long totalCount;
        
        // Apply filters if provided
        if (status != null && connectionId != null) {
            // Filter by both status and connection ID
            ScanMetadata.ScanStatus scanStatus = parseScanStatus(status);
            scanList = scanPersistenceService.getScansByStatusAndConnectionId(scanStatus, connectionId);
            totalCount = scanList.size();
            // Apply pagination manually
            scanList = applyPagination(scanList, page, size);
        } else if (status != null) {
            // Filter by status only
            ScanMetadata.ScanStatus scanStatus = parseScanStatus(status);
            scanList = scanPersistenceService.getScansByStatus(scanStatus);
            totalCount = scanList.size();
            // Apply pagination manually
            scanList = applyPagination(scanList, page, size);
        } else if (connectionId != null) {
            // Filter by connection ID only
            scanList = scanPersistenceService.getScansByConnectionId(connectionId);
            totalCount = scanList.size();
            // Apply pagination manually
            scanList = applyPagination(scanList, page, size);
        } else {
            // No filters, get paginated scans
            scanList = scanPersistenceService.getPagedScans(page, size);
            totalCount = scanPersistenceService.countAllScans();
        }
        
        // Convert ScanMetadata entities to ScanJobResponse DTOs
        List<ScanJobResponse> jobResponses = scanList.stream()
                .map(this::convertToJobResponse)
                .collect(Collectors.toList());
        
        // Create and populate the page response
        PageResponse<ScanJobResponse> pageResponse = new PageResponse<>();
        pageResponse.setContent(jobResponses);
        pageResponse.setPageNumber(page);
        pageResponse.setPageSize(size);
        pageResponse.setTotalElements(totalCount);
        
        // Calculate total pages
        int totalPages = (int) Math.ceil((double) totalCount / size);
        pageResponse.setTotalPages(totalPages);
        
        // Set pagination metadata
        pageResponse.setFirst(page == 0);
        pageResponse.setLast(page >= totalPages - 1);
        pageResponse.setHasNext(page < totalPages - 1);
        pageResponse.setHasPrevious(page > 0);
        
        return pageResponse;
    }

    /**
     * Gets the scan report as a DTO for serialization.
     *
     * @param jobId The job ID
     * @return The compliance report DTO
     */
    public ComplianceReportDTO getScanReportAsDTO(UUID jobId) {
        return (ComplianceReportDTO) scanReportService.getScanReportAsDTO(jobId);
    }

    /**
     * Gets the raw scan report data.
     *
     * @param jobId The job ID
     * @return The compliance report
     */
    public ComplianceReport getRawScanReport(UUID jobId) {
        return scanReportService.getScanReport(jobId);
    }

    /**
     * Exports a report in JSON format.
     *
     * @param jobId The job ID
     * @return The report as a JSON byte array
     */
    public byte[] exportReportAsJson(UUID jobId) {
        log.debug("Orchestrating JSON report export for job ID: {}", jobId);
        return enhancedReportExportService.exportReportAsJson(jobId);
    }

    /**
     * Exports a report in CSV format.
     *
     * @param jobId The job ID
     * @return The report as a CSV byte array
     */
    public byte[] exportReportAsCsv(UUID jobId) {
        log.debug("Orchestrating CSV report export for job ID: {}", jobId);
        return enhancedReportExportService.exportReportAsCsv(jobId);
    }

    /**
     * Exports a report in PDF format.
     *
     * @param jobId The job ID
     * @return The report as a PDF byte array
     */
    public byte[] exportReportAsPdf(UUID jobId) {
        log.debug("Orchestrating PDF report export for job ID: {}", jobId);
        return enhancedReportExportService.exportReportAsPdf(jobId);
    }

    /**
     * Exports a report in plain text format.
     *
     * @param jobId The job ID
     * @return The report as a text byte array
     */
    public byte[] exportReportAsText(UUID jobId) {
        log.debug("Orchestrating text report export for job ID: {}", jobId);
        return enhancedReportExportService.exportReportAsText(jobId);
    }

    /**
     * Exports a report in HTML format.
     *
     * @param jobId The job ID
     * @return The report as an HTML byte array
     */
    public byte[] exportReportAsHtml(UUID jobId) {
        log.debug("Orchestrating HTML report export for job ID: {}", jobId);
        return enhancedReportExportService.exportReportAsHtml(jobId);
    }

    /**
     * Exports a report in Excel format.
     *
     * @param jobId The job ID
     * @return The report as an Excel byte array
     */
    public byte[] exportReportAsExcel(UUID jobId) {
        log.debug("Orchestrating Excel report export for job ID: {}", jobId);
        return enhancedReportExportService.exportReportAsExcel(jobId);
    }

    /**
     * Exports a report in the specified format.
     *
     * @param jobId The job ID
     * @param format The format to export to (json, csv, pdf, text, html, excel)
     * @return The report in the requested format as a byte array
     */
    public byte[] exportReport(UUID jobId, String format) {
        log.debug("Orchestrating report export in {} format for job ID: {}", format, jobId);
        return enhancedReportExportService.exportReport(jobId, format);
    }

    /**
     * Exports a report in the specified format with custom options.
     *
     * @param jobId The job ID
     * @param format The format to export to (json, csv, pdf, text, html, excel)
     * @param options Map of formatting options
     * @return The report in the requested format as a byte array
     */
    public byte[] exportReport(UUID jobId, String format, Map<String, Object> options) {
        log.debug("Orchestrating report export in {} format with custom options for job ID: {}", format, jobId);
        return enhancedReportExportService.exportReport(jobId, format, options);
    }

    /**
     * Gets filtered scan detection results.
     *
     * @param jobId The job ID
     * @param piiType Optional filter for specific PII type
     * @param confidenceMin Optional minimum confidence score threshold
     * @param page The page number (0-based)
     * @param size The page size
     * @return List of filtered detection results
     */
    public List<DetectionResultDTO> getScanResultsWithFiltering(
            UUID jobId, String piiType, Double confidenceMin, int page, int size) {
        log.debug("Retrieving filtered detection results for job {}", jobId);
        
        // Verify the job exists and is completed
        ScanJobResponse jobStatus = getJobStatus(jobId);
        if (!jobStatus.isCompleted()) {
            throw new IllegalStateException("Cannot retrieve results: scan is not completed");
        }
        
        // Get all detection results for this scan
        List<DetectionResult> allResults = scanPersistenceService.getDetectionResultsByScanId(jobId);
        log.debug("Found {} total detection results", allResults.size());
        
        // Apply filters
        Stream<DetectionResult> filteredStream = allResults.stream();
        
        // Filter by PII type if specified
        if (piiType != null && !piiType.isEmpty()) {
            String normalizedPiiType = piiType.toUpperCase();
            filteredStream = filteredStream.filter(result -> 
                result.getHighestConfidencePiiType() != null && 
                result.getHighestConfidencePiiType().equalsIgnoreCase(normalizedPiiType));
            log.debug("Filtered by PII type: {}", normalizedPiiType);
        }
        
        // Filter by minimum confidence score if specified
        if (confidenceMin != null && confidenceMin > 0) {
            filteredStream = filteredStream.filter(result -> 
                result.getHighestConfidenceScore() >= confidenceMin);
            log.debug("Filtered by minimum confidence score: {}", confidenceMin);
        }
        
        // Sort by confidence score (highest first)
        List<DetectionResult> filteredResults = filteredStream
                .sorted(Comparator.comparing(DetectionResult::getHighestConfidenceScore).reversed())
                .collect(Collectors.toList());
        
        log.debug("After filtering, found {} results", filteredResults.size());
        
        // Apply pagination
        List<DetectionResult> pagedResults = applyPagination(filteredResults, page, size);
        
        // Convert to DTOs
        return pagedResults.stream()
                .map(entityMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets statistics about a scan.
     *
     * @param jobId The job ID
     * @return Scan statistics DTO
     */
    public ScanStatsDTO getScanStatistics(UUID jobId) {
        log.debug("Retrieving scan statistics for job {}", jobId);
        
        // Get the scan metadata
        ScanMetadata scanMetadata = scanPersistenceService.getScanById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scan job not found: " + jobId));
        
        // Get the compliance report if available
        ComplianceReport report = scanReportService.getScanReport(jobId);
        
        // Build the stats DTO
        ScanStatsDTO.ScanStatsDTOBuilder builder = ScanStatsDTO.builder()
                .scanId(scanMetadata.getId().toString())
                .databaseName(scanMetadata.getDatabaseName())
                .databaseProductName(scanMetadata.getDatabaseProductName())
                .databaseProductVersion(scanMetadata.getDatabaseProductVersion())
                .startTime(scanMetadata.getStartTime())
                .endTime(scanMetadata.getEndTime())
                .status(scanMetadata.getStatus().name())
                .completed(scanMetadata.getStatus() == ScanMetadata.ScanStatus.COMPLETED)
                .failed(scanMetadata.getStatus() == ScanMetadata.ScanStatus.FAILED);
        
        // Calculate scan duration if both start and end times are available
        if (scanMetadata.getStartTime() != null && scanMetadata.getEndTime() != null) {
            Duration duration = Duration.between(scanMetadata.getStartTime(), scanMetadata.getEndTime());
            builder.scanDuration(formatDuration(duration));
        }
        
        // Add information from the report if available
        if (report != null) {
            // Basic statistics
            builder.totalTablesScanned(report.getTotalTablesScanned())
                   .totalColumnsScanned(report.getTotalColumnsScanned())
                   .piiColumnsFound(report.getTotalPiiColumnsFound())
                   .quasiIdentifierColumnsFound(report.getTotalQuasiIdentifierColumnsFound());
            
            // Calculate PII type distribution
            Map<String, Integer> piiTypeDistribution = calculatePiiTypeDistribution(report);
            builder.piiTypeDistribution(piiTypeDistribution);
            
            // Total PII candidates
            int totalCandidates = report.getPiiFindings().stream()
                    .mapToInt(result -> result.getCandidates() != null ? result.getCandidates().size() : 0)
                    .sum();
            builder.totalPiiCandidates(totalCandidates);
        }
        
        return builder.build();
    }
    
    /**
     * Gets a list of tables that were scanned with PII statistics.
     *
     * @param jobId The job ID
     * @return List of scanned tables with statistics
     */
    public List<ScannedTableDTO> getScannedTables(UUID jobId) {
        log.debug("Retrieving scanned tables for job {}", jobId);
        
        // Verify the job exists
        scanPersistenceService.getScanById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scan job not found: " + jobId));
        
        // Get the compliance report
        ComplianceReport report = scanReportService.getScanReport(jobId);
        
        // Group detection results by table
        Map<String, List<DetectionResult>> resultsByTable = report.getDetectionResults().stream()
                .filter(result -> result.getColumnInfo() != null && result.getColumnInfo().getTable() != null)
                .collect(Collectors.groupingBy(result -> result.getColumnInfo().getTable().getTableName()));
        
        // Build DTOs for each table
        List<ScannedTableDTO> tables = new ArrayList<>();
        
        for (Map.Entry<String, List<DetectionResult>> entry : resultsByTable.entrySet()) {
            String tableName = entry.getKey();
            List<DetectionResult> tableResults = entry.getValue();
            
            // Get the first result to extract table info
            DetectionResult sampleResult = tableResults.get(0);
            TableInfo tableInfo = sampleResult.getColumnInfo().getTable();
            
            // Count PII columns
            long piiColumnCount = tableResults.stream()
                    .filter(DetectionResult::hasPii)
                    .count();
            
            // Count quasi-identifier columns
            long qiColumnCount = tableResults.stream()
                    .filter(DetectionResult::isQuasiIdentifier)
                    .count();
            
            // Count risk levels
            long highRiskCount = tableResults.stream()
                    .filter(result -> result.getHighestConfidenceScore() >= 0.8)
                    .count();
                    
            long mediumRiskCount = tableResults.stream()
                    .filter(result -> result.getHighestConfidenceScore() >= 0.5 && result.getHighestConfidenceScore() < 0.8)
                    .count();
                    
            long lowRiskCount = tableResults.stream()
                    .filter(result -> result.getHighestConfidenceScore() > 0 && result.getHighestConfidenceScore() < 0.5)
                    .count();
            
            // Calculate PII type distribution for this table
            Map<String, Integer> piiTypeDistribution = tableResults.stream()
                    .filter(result -> result.getHighestConfidencePiiType() != null)
                    .collect(Collectors.groupingBy(
                        DetectionResult::getHighestConfidencePiiType,
                        Collectors.summingInt(result -> 1)
                    ));
            
            // Build the table DTO
            ScannedTableDTO tableDto = ScannedTableDTO.builder()
                    .id(tableInfo.getId())
                    .tableName(tableName)
                    .schemaName(tableInfo.getSchema() != null ? tableInfo.getSchema().getSchemaName() : null)
                    .qualifiedName(tableInfo.getSchema() != null ? 
                            tableInfo.getSchema().getSchemaName() + "." + tableName : tableName)
                    .totalColumns(tableResults.size())
                    .piiColumnCount((int)piiColumnCount)
                    .quasiIdentifierColumnCount((int)qiColumnCount)
                    .highRiskColumnCount((int)highRiskCount)
                    .mediumRiskColumnCount((int)mediumRiskCount)
                    .lowRiskColumnCount((int)lowRiskCount)
                    .piiTypeDistribution(piiTypeDistribution)
                    .build();
            
            tables.add(tableDto);
        }
        
        return tables;
    }
    
    /**
     * Gets a list of columns from a specific table with PII detection results.
     *
     * @param jobId The job ID
     * @param tableName The table name
     * @return List of scanned columns with PII details
     */
    @Transactional(readOnly = true)
    public List<ScannedColumnDTO> getScannedColumns(UUID jobId, String tableName) {
        log.debug("Retrieving scanned columns for job {} and table {}", jobId, tableName);
        
        // Verify the job exists
        scanPersistenceService.getScanById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scan job not found: " + jobId));
        
        // Get the compliance report
        ComplianceReport report = scanReportService.getScanReport(jobId);
        
        // Filter detection results for the specified table
        List<DetectionResult> tableResults = report.getDetectionResults().stream()
                .filter(result -> result.getColumnInfo() != null && 
                       result.getColumnInfo().getTable() != null &&
                       tableName.equals(result.getColumnInfo().getTable().getTableName()))
                .collect(Collectors.toList());
        
        if (tableResults.isEmpty()) {
            throw new ResourceNotFoundException("Table not found in scan results: " + tableName);
        }
        
        // Convert the results to DTOs
        return tableResults.stream()
                .map(this::mapToScannedColumnDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps a DetectionResult to a ScannedColumnDTO.
     */
    private ScannedColumnDTO mapToScannedColumnDTO(DetectionResult result) {
        ColumnInfo columnInfo = result.getColumnInfo();
        TableInfo tableInfo = columnInfo.getTable();
        String schemaName = tableInfo.getSchema() != null ? tableInfo.getSchema().getSchemaName() : null;
        String tableName = tableInfo.getTableName();
        
        // Determine risk level based on confidence score
        String riskLevel = "NONE";
        if (result.getHighestConfidenceScore() >= 0.8) {
            riskLevel = "HIGH";
        } else if (result.getHighestConfidenceScore() >= 0.5) {
            riskLevel = "MEDIUM";
        } else if (result.getHighestConfidenceScore() > 0) {
            riskLevel = "LOW";
        }
        
        // Extract detection methods from candidates - safely handle lazy loading
        List<String> detectionMethods = Collections.emptyList();
        try {
            // First check if the collection is initialized before accessing it
            if (result.getCandidates() != null && Hibernate.isInitialized(result.getCandidates()) && !result.getCandidates().isEmpty()) {
                detectionMethods = result.getCandidates().stream()
                        .map(candidate -> candidate.getDetectionMethod())
                        .distinct()
                        .collect(Collectors.toList());
            } else {
                log.debug("Candidates collection not initialized for result {}", result.getId());
            }
        } catch (Exception e) {
            log.warn("Error accessing candidates for result {}: {}", result.getId(), e.getMessage());
        }
        
        return ScannedColumnDTO.builder()
                .id(columnInfo.getId())
                .tableId(tableInfo.getId())
                .columnName(columnInfo.getColumnName())
                .dataType(columnInfo.getDatabaseTypeName()) // Use databaseTypeName instead of getDataType()
                .qualifiedName(schemaName != null ? 
                        schemaName + "." + tableName + "." + columnInfo.getColumnName() : 
                        tableName + "." + columnInfo.getColumnName())
                .hasPii(result.hasPii())
                .isQuasiIdentifier(result.isQuasiIdentifier())
                .highestConfidencePiiType(result.getHighestConfidencePiiType())
                .confidenceScore(result.getHighestConfidenceScore())
                .detectionMethods(detectionMethods)
                .riskLevel(riskLevel)
                .correlatedColumns(result.getCorrelatedColumns())
                .clusteringMethod(result.getClusteringMethod())
                // Use getAttribute for entropy or provide a default value if not available
                .entropy(result.getAttribute("entropy") != null ? 
                       (Double)result.getAttribute("entropy") : null)
                .build();
    }
    
    /**
     * Calculate PII type distribution from a compliance report.
     */
    private Map<String, Integer> calculatePiiTypeDistribution(ComplianceReport report) {
        return report.getPiiFindings().stream()
                .filter(result -> result.getHighestConfidencePiiType() != null)
                .collect(Collectors.groupingBy(
                    DetectionResult::getHighestConfidencePiiType,
                    Collectors.summingInt(result -> 1)
                ));
    }
    
    /**
     * Format a duration into a human-readable string.
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%d hours, %d minutes, %d seconds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d minutes, %d seconds", minutes, seconds);
        } else {
            return String.format("%d seconds", seconds);
        }
    }

    /**
     * Helper method to parse string status to enum.
     */
    private ScanMetadata.ScanStatus parseScanStatus(String status) {
        if (status == null) {
            return null;
        }
        
        switch (status.toLowerCase()) {
            case "completed":
                return ScanMetadata.ScanStatus.COMPLETED;
            case "pending":
                return ScanMetadata.ScanStatus.PENDING;
            case "failed":
                return ScanMetadata.ScanStatus.FAILED;
            case "cancelled":
                return ScanMetadata.ScanStatus.CANCELLED;
            case "sampling":
                return ScanMetadata.ScanStatus.SAMPLING;
            case "extracting_metadata":
                return ScanMetadata.ScanStatus.EXTRACTING_METADATA;
            case "detecting_pii":
                return ScanMetadata.ScanStatus.DETECTING_PII;
            case "generating_report":
                return ScanMetadata.ScanStatus.GENERATING_REPORT;
            default:
                throw new IllegalArgumentException("Invalid scan status: " + status);
        }
    }
    
    /**
     * Helper method to convert ScanMetadata to ScanJobResponse.
     */
    private ScanJobResponse convertToJobResponse(ScanMetadata scan) {
        ScanJobResponse response = new ScanJobResponse();
        response.setJobId(scan.getId());
        response.setConnectionId(scan.getConnectionId());
        response.setStartTime(scan.getStartTime() != null ? 
                scan.getStartTime().toString() : null);
        response.setEndTime(scan.getEndTime() != null ? 
                scan.getEndTime().toString() : null);
        
        // Map the status
        String statusStr;
        boolean completed = false;
        boolean failed = false;
        
        switch (scan.getStatus()) {
            case PENDING:
                statusStr = "PENDING";
                break;
            case EXTRACTING_METADATA:
                statusStr = "EXTRACTING_METADATA";
                break;
            case SAMPLING:
                statusStr = "SAMPLING";
                break;
            case DETECTING_PII:
                statusStr = "DETECTING_PII";
                break;
            case GENERATING_REPORT:
                statusStr = "GENERATING_REPORT";
                break;
            case COMPLETED:
                statusStr = "COMPLETED";
                completed = true;
                // Set metadata status to SUCCESS for completed scans to ensure isSuccess() returns true
                response.addMeta("status", "SUCCESS");
                break;
            case FAILED:
                statusStr = "FAILED";
                failed = true;
                break;
            case CANCELLED:
                statusStr = "CANCELLED";
                failed = true;
                break;
            default:
                statusStr = "UNKNOWN";
        }
        
        response.setStatus(statusStr);
        response.setCompleted(completed);
        response.setFailed(failed);
        response.setErrorMessage(scan.getErrorMessage());
        
        // Set scan results summary
        response.setDatabaseName(scan.getDatabaseName());
        response.setDatabaseProductName(scan.getDatabaseProductName());
        response.setTotalColumnsScanned(scan.getTotalColumnsScanned());
        response.setTotalPiiColumnsFound(scan.getTotalPiiColumnsFound());
        
        return response;
    }
    
    /**
     * Helper method to manually apply pagination to a list.
     */
    private <T> List<T> applyPagination(List<T> list, int page, int size) {
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        
        int fromIndex = page * size;
        if (fromIndex >= list.size()) {
            return new ArrayList<>();
        }
        
        int toIndex = Math.min(fromIndex + size, list.size());
        return list.subList(fromIndex, toIndex);
    }
}