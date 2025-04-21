package com.privsense.api.service;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.config.SamplingConfigProperties;
import com.privsense.api.config.DetectionConfigProperties;
import com.privsense.core.repository.ConnectionRepository;
import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.model.*;
import com.privsense.core.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.ArrayList;


/**
 * Service responsible for orchestrating the scan process.
 * Manages the asynchronous execution of database scans using persistent storage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanOrchestrationService {

    /**
     * Enum representing the possible states of a scan job.
     */
    public enum JobState {
        PENDING,
        EXTRACTING_METADATA,
        SAMPLING,
        DETECTING_PII,
        GENERATING_REPORT,
        COMPLETED,
        FAILED
    }

    /**
     * Class representing the status of a scan job.
     */
    public static class JobStatus {
        private final UUID jobId;
        private final UUID connectionId;
        private JobState state;
        private final LocalDateTime startTime;
        private LocalDateTime lastUpdateTime;
        private String errorMessage;
        private ComplianceReport report;

        public JobStatus(UUID jobId, UUID connectionId) {
            this.jobId = jobId;
            this.connectionId = connectionId;
            this.state = JobState.PENDING;
            this.startTime = LocalDateTime.now();
            this.lastUpdateTime = LocalDateTime.now();
        }

        public UUID getJobId() {
            return jobId;
        }

        public UUID getConnectionId() {
            return connectionId;
        }

        public JobState getState() {
            return state;
        }

        public void setState(JobState state) {
            this.state = state;
            this.lastUpdateTime = LocalDateTime.now();
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getLastUpdateTime() {
            return lastUpdateTime;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public ComplianceReport getReport() {
            return report;
        }

        public void setReport(ComplianceReport report) {
            this.report = report;
        }
    }

    // Dependency injection of required services
    private final DatabaseConnector databaseConnector;
    private final MetadataExtractor metadataExtractor;
    private final Sampler sampler;
    private final PiiDetector piiDetector;
    private final ReportGenerator reportGenerator;
    private final ConnectionRepository connectionRepository;
    private final SamplingConfigProperties samplingConfigProps;
    private final DetectionConfigProperties detectionConfigProps;
    private final ModelMapper modelMapper;
    private final ScanPersistenceService scanPersistenceService;

    /**
     * Submits a new scan job and returns a unique job identifier.
     *
     * @param scanRequest The scan configuration
     * @return A UUID identifying the scan job
     */
    public UUID submitScanJob(ScanRequest scanRequest) {
        // Validate connection ID exists
        if (!connectionRepository.existsById(scanRequest.getConnectionId())) {
            throw new IllegalArgumentException("Connection ID not found: " + scanRequest.getConnectionId());
        }

        // Create initial scan record in the database
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(scanRequest.getConnectionId())
            .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + scanRequest.getConnectionId()));
            
        ScanMetadata scanMetadata = scanPersistenceService.createScan(
            scanRequest.getConnectionId(), 
            connectionInfo.getDatabaseName(),
            "Pending", // Will be updated when we get the actual connection
            "Pending"  // Will be updated when we get the actual connection
        );
        
        UUID jobId = scanMetadata.getId();
        
        // Start the asynchronous scan process
        processScanAsync(jobId, scanRequest);

        return jobId;
    }

    /**
     * Processes the scan job asynchronously.
     * This method is executed in a separate thread managed by Spring's TaskExecutor.
     *
     * @param jobId The UUID of the job
     * @param request The scan request details
     */
    @Async("taskExecutor")
    protected CompletableFuture<Void> processScanAsync(UUID jobId, ScanRequest request) {
        try {
            // 1. Get connection ID
            UUID connectionId = request.getConnectionId();
            
            // Update scan status to EXTRACTING_METADATA
            scanPersistenceService.updateScanStatus(jobId, 
                ScanMetadata.ScanStatus.EXTRACTING_METADATA);
                
            log.info("Job {}: Extracting metadata from database", jobId);

            // Verify connection exists before trying to get a connection object
            if (!connectionRepository.existsById(connectionId)) {
                throw new IllegalArgumentException("Connection ID not found: " + connectionId);
            }

            try (Connection connection = databaseConnector.getConnection(connectionId)) { 
                // Update database info in the scan record
                String dbName = connection.getCatalog();
                String dbProductName = connection.getMetaData().getDatabaseProductName();
                String dbProductVersion = connection.getMetaData().getDatabaseProductVersion();
                
                // Update the scan record with actual database info
                ScanMetadata scanMetadata = scanPersistenceService.getScanById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Scan record not found: " + jobId));
                    
                // TODO: Add method to update database info in ScanPersistenceService
                
                // 2. Extract metadata
                SchemaInfo schemaInfo = metadataExtractor.extractMetadata(connection);
                List<TableInfo> targetTables = filterTargetTables(schemaInfo, request.getTargetTables());

                // Update scan status to SAMPLING
                scanPersistenceService.updateScanStatus(jobId, 
                    ScanMetadata.ScanStatus.SAMPLING);
                    
                log.info("Job {}: Sampling data from columns", jobId);

                // 3. Sample data
                List<ColumnInfo> columnsToSample = getAllColumns(targetTables);
                SamplingConfig samplingConfig = buildSamplingConfig(request);

                Map<ColumnInfo, SampleData> sampledData = sampler.extractSamples(
                        connection,
                        columnsToSample,
                        samplingConfig
                );

                // Update scan status to DETECTING_PII
                scanPersistenceService.updateScanStatus(jobId, 
                    ScanMetadata.ScanStatus.DETECTING_PII);
                    
                log.info("Job {}: Detecting PII in sampled data", jobId);

                // 4. Detect PII
                DetectionConfig detectionConfig = buildDetectionConfig(request);
                List<DetectionResult> detectionResults = piiDetector.detectPii(sampledData);
                
                // Save detection results to database
                scanPersistenceService.saveScanResults(jobId, detectionResults);

                // Update scan status to GENERATING_REPORT
                scanPersistenceService.updateScanStatus(jobId, 
                    ScanMetadata.ScanStatus.GENERATING_REPORT);
                    
                log.info("Job {}: Generating compliance report", jobId);

                // 5. Generate report
                // Need DatabaseConnectionInfo for the report context
                DatabaseConnectionInfo connectionInfoForReport = connectionRepository.findById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection ID not found for report context: " + connectionId));

                ScanContext scanContext = ScanContext.builder()
                        .databaseConnectionInfo(connectionInfoForReport)
                        .schemaInfo(schemaInfo)
                        .sampledData(sampledData)
                        .detectionResults(detectionResults)
                        .samplingConfig(samplingConfig)
                        .detectionConfig(detectionConfig)
                        .scanStartTime(LocalDateTime.now())
                        .build();

                ComplianceReport report = reportGenerator.generateReport(scanContext);

                // Update scan status to COMPLETED
                scanPersistenceService.completeScan(jobId);
                
                log.info("Job {}: Scan completed successfully", jobId);
            }

        } catch (DatabaseConnectionException e) {
            log.error("Job {}: Database connection failed", jobId, e);
            scanPersistenceService.failScan(jobId, "Database connection error: " + e.getMessage());
        } catch (MetadataExtractionException e) {
            log.error("Job {}: Metadata extraction failed", jobId, e);
            scanPersistenceService.failScan(jobId, "Metadata extraction error: " + e.getMessage());
        } catch (PiiDetectionException e) {
            log.error("Job {}: PII detection failed", jobId, e);
            scanPersistenceService.failScan(jobId, "PII detection error: " + e.getMessage());
        } catch (SQLException e) {
            log.error("Job {}: SQL error during scan", jobId, e);
            scanPersistenceService.failScan(jobId, "SQL error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Job {}: Scan failed with unexpected error", jobId, e);
            scanPersistenceService.failScan(jobId, "Unexpected error: " + e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Gets the current status of a scan job.
     *
     * @param jobId The UUID of the job
     * @return A ScanJobResponse with current status information
     * @throws IllegalArgumentException if the job ID is not found
     */
    public ScanJobResponse getJobStatus(UUID jobId) {
        // Get from the database
        Optional<ScanMetadata> scanOpt = scanPersistenceService.getScanById(jobId);
        if (!scanOpt.isPresent()) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        ScanMetadata scan = scanOpt.get();
        JobStatus status = new JobStatus(scan.getId(), scan.getConnectionId());
        
        // Convert database status to in-memory status
        switch (scan.getStatus()) {
            case PENDING:
                status.setState(JobState.PENDING);
                break;
            case EXTRACTING_METADATA:
                status.setState(JobState.EXTRACTING_METADATA);
                break;
            case SAMPLING:
                status.setState(JobState.SAMPLING);
                break;
            case DETECTING_PII:
                status.setState(JobState.DETECTING_PII);
                break;
            case GENERATING_REPORT:
                status.setState(JobState.GENERATING_REPORT);
                break;
            case COMPLETED:
                status.setState(JobState.COMPLETED);
                break;
            case FAILED:
                status.setState(JobState.FAILED);
                status.setErrorMessage(scan.getErrorMessage());
                break;
            default:
                status.setState(JobState.PENDING);
        }
        
        // Use ModelMapper to convert JobStatus to ScanJobResponse
        return modelMapper.map(status, ScanJobResponse.class);
    }

    /**
     * Gets the completed scan report.
     *
     * @param jobId The UUID of the job
     * @return The ComplianceReport for the completed scan
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the scan is not complete
     */
    public ComplianceReport getScanReport(UUID jobId) {
        // Get job status to check if job is completed
        try {
            ScanJobResponse jobResponse = getJobStatus(jobId);
            if (!jobResponse.isCompleted()) {
                throw new IllegalStateException("Scan job is not completed. Current state: " + jobResponse.getStatus());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        // Get scan data from the database
        Optional<ScanMetadata> scanOpt = scanPersistenceService.getScanById(jobId);
        if (!scanOpt.isPresent()) {
            throw new IllegalArgumentException("Scan data not found in database: " + jobId);
        }
        
        ScanMetadata scan = scanOpt.get();
        
        // Get detection results from database
        List<DetectionResult> detectionResults = scanPersistenceService.getPiiResultsByScanId(jobId);

        // Build a report from the database results
        // We need the database connection info for the report
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(scan.getConnectionId())
            .orElseThrow(() -> new IllegalArgumentException("Connection ID not found: " + scan.getConnectionId()));
            
        // Create a report with the database results
        return ComplianceReport.builder()
            .scanId(scan.getId())
            .reportId(UUID.randomUUID().toString())
            .connectionInfo(connectionInfo)
            .piiFindings(detectionResults)
            .totalColumnsScanned(scan.getTotalColumnsScanned())
            .totalPiiColumnsFound(scan.getTotalPiiColumnsFound())
            .scanStartTime(scan.getStartTime().atZone(ZoneId.systemDefault()).toInstant())
            .scanEndTime(scan.getEndTime() != null ? scan.getEndTime().atZone(ZoneId.systemDefault()).toInstant() : null)
            .databaseName(scan.getDatabaseName())
            .databaseProductName(scan.getDatabaseProductName())
            .databaseProductVersion(scan.getDatabaseProductVersion())
            .build();
    }

    /**
     * Gets the completed scan report as a DTO to avoid LazyInitializationException.
     *
     * @param jobId The UUID of the job
     * @return The ComplianceReportDTO for the completed scan
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the scan is not complete
     */
    @Transactional(readOnly = true)
    public ComplianceReportDTO getScanReportAsDTO(UUID jobId) {
        // First get the ComplianceReport entity (which is done inside a transaction)
        ComplianceReport report = getScanReport(jobId);
        
        // Create and populate the DTO - all lazy collections are accessed within transaction
        ComplianceReportDTO dto = ComplianceReportDTO.builder()
            .scanId(report.getScanId())
            .reportId(report.getReportId())
            .databaseHost(report.getDatabaseHost())
            .databaseName(report.getDatabaseName())
            .databaseProductName(report.getDatabaseProductName())
            .databaseProductVersion(report.getDatabaseProductVersion())
            .totalTablesScanned(report.getTotalTablesScanned())
            .totalColumnsScanned(report.getTotalColumnsScanned())
            .totalPiiColumnsFound(report.getTotalPiiColumnsFound())
            .scanStartTime(report.getScanStartTime())
            .scanEndTime(report.getScanEndTime())
            .scanDuration(report.getScanDuration())
            .samplingConfig(report.getSamplingConfig())
            .detectionConfig(report.getDetectionConfig())
            .build();
            
        // Convert and add PII findings
        List<ComplianceReportDTO.PiiColumnDTO> piiFindings = new ArrayList<>();
        for (DetectionResult result : report.getPiiFindings()) {
            // Access lazy-loaded collections within transaction
            List<String> methods = new ArrayList<>(result.getDetectionMethods());
            
            piiFindings.add(ComplianceReportDTO.PiiColumnDTO.builder()
                .tableName(result.getColumnInfo().getTable().getTableName())
                .columnName(result.getColumnInfo().getColumnName())
                .dataType(result.getColumnInfo().getDatabaseTypeName())
                .piiType(result.getHighestConfidencePiiType())
                .confidenceScore(result.getHighestConfidenceScore())
                .detectionMethods(methods)
                .build());
        }
        dto.setPiiFindings(piiFindings);
        
        // If there's a summary, copy it
        if (report.getSummary() != null) {
            ComplianceReportDTO.ScanSummaryDTO summary = ComplianceReportDTO.ScanSummaryDTO.builder()
                .tablesScanned(report.getSummary().getTablesScanned())
                .columnsScanned(report.getSummary().getColumnsScanned())
                .piiColumnsFound(report.getSummary().getPiiColumnsFound())
                .totalPiiCandidates(report.getSummary().getTotalPiiCandidates())
                .scanDurationMillis(report.getSummary().getScanDurationMillis())
                .build();
            // Note: The DTO doesn't have a setSummary method defined, so we're not setting it
        }
        
        return dto;
    }

    /**
     * Cancels a scan job if it's still in progress.
     *
     * @param jobId The UUID of the job to cancel
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the job is already completed or failed
     */
    public void cancelScan(UUID jobId) {
        Optional<ScanMetadata> scanOpt = scanPersistenceService.getScanById(jobId);
        if (!scanOpt.isPresent()) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        ScanMetadata scan = scanOpt.get();
        
        if (scan.getStatus() == ScanMetadata.ScanStatus.COMPLETED || 
            scan.getStatus() == ScanMetadata.ScanStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel job that is already " + scan.getStatus());
        }

        scanPersistenceService.failScan(jobId, "Job cancelled by user request");
        log.info("Job {}: Cancelled by user request", jobId);
    }

    /**
     * Exports the scan report as CSV.
     *
     * @param jobId The UUID of the job
     * @return The report content as CSV byte array
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the scan is not complete
     */
    @Transactional
    public byte[] exportReportAsCsv(UUID jobId) {
        // Check if job is completed first
        try {
            ScanJobResponse jobResponse = getJobStatus(jobId);
            if (!jobResponse.isCompleted()) {
                throw new IllegalStateException("Scan job is not completed. Current state: " + jobResponse.getStatus());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        // Eager loading of DetectionResult with detectionMethods to avoid LazyInitializationException
        List<DetectionResult> detectionResults = scanPersistenceService.getPiiResultsByScanId(jobId);
        
        // Force initialization of detectionMethods collection for each result
        for (DetectionResult result : detectionResults) {
            if (result != null) {
                // Access the collection to force initialization while session is still open
                result.getDetectionMethods().size();
            }
        }
        
        ScanMetadata scan = scanPersistenceService.getScanById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Scan data not found in database: " + jobId));
            
        // Build report with the eagerly loaded detection results
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(scan.getConnectionId())
            .orElseThrow(() -> new IllegalArgumentException("Connection ID not found: " + scan.getConnectionId()));
            
        ComplianceReport report = ComplianceReport.builder()
            .scanId(scan.getId())
            .reportId(UUID.randomUUID().toString())
            .connectionInfo(connectionInfo)
            .piiFindings(detectionResults)
            .totalColumnsScanned(scan.getTotalColumnsScanned())
            .totalPiiColumnsFound(scan.getTotalPiiColumnsFound())
            .scanStartTime(scan.getStartTime().atZone(ZoneId.systemDefault()).toInstant())
            .scanEndTime(scan.getEndTime() != null ? scan.getEndTime().atZone(ZoneId.systemDefault()).toInstant() : null)
            .databaseHost(connectionInfo.getHost())
            .databaseName(scan.getDatabaseName())
            .databaseProductName(scan.getDatabaseProductName())
            .databaseProductVersion(scan.getDatabaseProductVersion())
            .build();
        
        // Get the report generator to convert the report to CSV
        return reportGenerator.exportReportAsCsv(report);
    }

    /**
     * Exports the scan report as plain text.
     *
     * @param jobId The UUID of the job
     * @return The report content as text byte array
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the scan is not complete
     */
    @Transactional
    public byte[] exportReportAsText(UUID jobId) {
        // Check if job is completed first
        try {
            ScanJobResponse jobResponse = getJobStatus(jobId);
            if (!jobResponse.isCompleted()) {
                throw new IllegalStateException("Scan job is not completed. Current state: " + jobResponse.getStatus());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        // Eager loading of DetectionResult with detectionMethods to avoid LazyInitializationException
        List<DetectionResult> detectionResults = scanPersistenceService.getPiiResultsByScanId(jobId);
        
        // Force initialization of detectionMethods collection for each result
        for (DetectionResult result : detectionResults) {
            if (result != null) {
                // Access the collection to force initialization while session is still open
                result.getDetectionMethods().size();
            }
        }
        
        ScanMetadata scan = scanPersistenceService.getScanById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Scan data not found in database: " + jobId));
            
        // Build report with the eagerly loaded detection results
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(scan.getConnectionId())
            .orElseThrow(() -> new IllegalArgumentException("Connection ID not found: " + scan.getConnectionId()));
            
        ComplianceReport report = ComplianceReport.builder()
            .scanId(scan.getId())
            .reportId(UUID.randomUUID().toString())
            .connectionInfo(connectionInfo)
            .piiFindings(detectionResults)
            .totalColumnsScanned(scan.getTotalColumnsScanned())
            .totalPiiColumnsFound(scan.getTotalPiiColumnsFound())
            .scanStartTime(scan.getStartTime().atZone(ZoneId.systemDefault()).toInstant())
            .scanEndTime(scan.getEndTime() != null ? scan.getEndTime().atZone(ZoneId.systemDefault()).toInstant() : null)
            .databaseHost(connectionInfo.getHost())
            .databaseName(scan.getDatabaseName())
            .databaseProductName(scan.getDatabaseProductName())
            .databaseProductVersion(scan.getDatabaseProductVersion())
            .build();
        
        // Get the report generator to convert the report to plain text
        return reportGenerator.exportReportAsText(report);
    }

    /**
     * Exports the scan report as PDF.
     *
     * @param jobId The UUID of the job
     * @return The report content as PDF byte array
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the scan is not complete
     */
    @Transactional
    public byte[] exportReportAsPdf(UUID jobId) {
        // Check if job is completed first
        try {
            ScanJobResponse jobResponse = getJobStatus(jobId);
            if (!jobResponse.isCompleted()) {
                throw new IllegalStateException("Scan job is not completed. Current state: " + jobResponse.getStatus());
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }
        
        // Eager loading of DetectionResult with detectionMethods to avoid LazyInitializationException
        List<DetectionResult> detectionResults = scanPersistenceService.getPiiResultsByScanId(jobId);
        
        // Force initialization of detectionMethods collection for each result
        for (DetectionResult result : detectionResults) {
            if (result != null) {
                // Access the collection to force initialization while session is still open
                result.getDetectionMethods().size();
            }
        }
        
        ScanMetadata scan = scanPersistenceService.getScanById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Scan data not found in database: " + jobId));
            
        // Build report with the eagerly loaded detection results
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(scan.getConnectionId())
            .orElseThrow(() -> new IllegalArgumentException("Connection ID not found: " + scan.getConnectionId()));
            
        ComplianceReport report = ComplianceReport.builder()
            .scanId(scan.getId())
            .reportId(UUID.randomUUID().toString())
            .connectionInfo(connectionInfo)
            .piiFindings(detectionResults)
            .totalColumnsScanned(scan.getTotalColumnsScanned())
            .totalPiiColumnsFound(scan.getTotalPiiColumnsFound())
            .scanStartTime(scan.getStartTime().atZone(ZoneId.systemDefault()).toInstant())
            .scanEndTime(scan.getEndTime() != null ? scan.getEndTime().atZone(ZoneId.systemDefault()).toInstant() : null)
            .databaseHost(connectionInfo.getHost())
            .databaseName(scan.getDatabaseName())
            .databaseProductName(scan.getDatabaseProductName())
            .databaseProductVersion(scan.getDatabaseProductVersion())
            .build();
        
        // Get the report generator to convert the report to PDF
        return reportGenerator.exportReportAsPdf(report);
    }

    /**
     * Helper method to filter tables if target tables are specified in the request.
     */
    private List<TableInfo> filterTargetTables(SchemaInfo schema, List<String> targetTableNames) {
        if (targetTableNames == null || targetTableNames.isEmpty()) {
            return schema.getTables();
        }

        return schema.getTables().stream()
                .filter(table -> targetTableNames.contains(table.getTableName()))
                .collect(Collectors.toList());
    }

    /**
     * Helper method to extract all columns from a list of tables.
     */
    private List<ColumnInfo> getAllColumns(List<TableInfo> tables) {
        return tables.stream()
                .flatMap(table -> table.getColumns().stream())
                .collect(Collectors.toList());
    }

    /**
     * Helper method to build a SamplingConfig from the request and default properties.
     */
    private SamplingConfig buildSamplingConfig(ScanRequest request) {
        return SamplingConfig.builder()
                .sampleSize(request.getSampleSize() != null ?
                        request.getSampleSize() : samplingConfigProps.getDefaultSize())
                .samplingMethod(request.getSamplingMethod() != null ?
                        request.getSamplingMethod() : samplingConfigProps.getMethods().getDefault())
                .maxConcurrentQueries(samplingConfigProps.getMaxConcurrentDbQueries())
                .build();
    }

    /**
     * Helper method to build a DetectionConfig from the request and default properties.
     */
    private DetectionConfig buildDetectionConfig(ScanRequest request) {
        return DetectionConfig.builder()
                .heuristicThreshold(request.getHeuristicThreshold() != null ?
                        request.getHeuristicThreshold() : detectionConfigProps.getThresholds().getHeuristic())
                .regexThreshold(request.getRegexThreshold() != null ?
                        request.getRegexThreshold() : detectionConfigProps.getThresholds().getRegex())
                .nerThreshold(request.getNerThreshold() != null ?
                        request.getNerThreshold() : detectionConfigProps.getThresholds().getNer())
                .reportingThreshold(detectionConfigProps.getThresholds().getReporting())
                .stopPipelineOnHighConfidence(detectionConfigProps.isStopPipelineOnHighConfidence())
                .entropyCalculationEnabled(request.getEntropyCalculationEnabled() != null ? 
                        request.getEntropyCalculationEnabled() : detectionConfigProps.isEntropyEnabled())
                .build();
    }
}