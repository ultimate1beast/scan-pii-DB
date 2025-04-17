package com.privsense.api.service;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.config.SamplingConfigProperties;
import com.privsense.api.config.DetectionConfigProperties;
import com.privsense.api.repository.ConnectionRepository;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * Service responsible for orchestrating the scan process.
 * Manages the asynchronous execution of database scans.
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

    // Map of job ID to job status
    private final ConcurrentHashMap<UUID, JobStatus> jobs = new ConcurrentHashMap<>();

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

    private final Map<UUID, JobStatus> jobStatusMap = new ConcurrentHashMap<>();
    private final Map<UUID, ComplianceReport> jobResultMap = new ConcurrentHashMap<>();

    /**
     * Start a new scan job.
     *
     * @param connectionId The ID of the database connection to scan
     * @param targetTables Optional list of specific tables to scan
     * @param samplingConfig Configuration for data sampling
     * @param detectionConfig Configuration for PII detection
     * @return The ID of the newly created job
     */
    public UUID startScanJob(UUID connectionId, List<String> targetTables,
                             SamplingConfig samplingConfig, DetectionConfig detectionConfig) {
        UUID jobId = UUID.randomUUID();
        JobStatus status = new JobStatus(jobId, connectionId);
        jobs.put(jobId, status);

        // In a real implementation, we would start an async task here

        return jobId;
    }

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

        UUID jobId = UUID.randomUUID();
        JobStatus jobStatus = new JobStatus(jobId, scanRequest.getConnectionId());
        jobStatusMap.put(jobId, jobStatus);

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
        JobStatus status = jobStatusMap.get(jobId);

        try {
            // 1. Get connection ID
            UUID connectionId = request.getConnectionId();
            status.setState(JobState.EXTRACTING_METADATA);
            log.info("Job {}: Extracting metadata from database", jobId);

            // Verify connection exists before trying to get a connection object
            if (!connectionRepository.existsById(connectionId)) {
                throw new IllegalArgumentException("Connection ID not found: " + connectionId);
            }

            try (Connection connection = databaseConnector.getConnection(connectionId)) { // Pass UUID
                // 2. Extract metadata
                SchemaInfo schemaInfo = metadataExtractor.extractMetadata(connection);
                List<TableInfo> targetTables = filterTargetTables(schemaInfo, request.getTargetTables());

                // 3. Sample data
                status.setState(JobState.SAMPLING);
                log.info("Job {}: Sampling data from columns", jobId);

                List<ColumnInfo> columnsToSample = getAllColumns(targetTables);
                SamplingConfig samplingConfig = buildSamplingConfig(request);

                Map<ColumnInfo, SampleData> sampledData = sampler.extractSamples(
                        connection,
                        columnsToSample,
                        samplingConfig
                );

                // 4. Detect PII
                status.setState(JobState.DETECTING_PII);
                log.info("Job {}: Detecting PII in sampled data", jobId);

                DetectionConfig detectionConfig = buildDetectionConfig(request);
                // Pass the map directly instead of converting to a list
                List<DetectionResult> detectionResults = piiDetector.detectPii(sampledData);

                // 5. Generate report
                status.setState(JobState.GENERATING_REPORT);
                log.info("Job {}: Generating compliance report", jobId);

                // Need DatabaseConnectionInfo for the report context
                DatabaseConnectionInfo connectionInfoForReport = connectionRepository.findById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection ID not found for report context: " + connectionId));

                ScanContext scanContext = ScanContext.builder()
                        .databaseConnectionInfo(connectionInfoForReport) // Use fetched connectionInfo
                        .schemaInfo(schemaInfo)
                        .sampledData(sampledData)
                        .detectionResults(detectionResults)
                        .samplingConfig(samplingConfig)
                        .detectionConfig(detectionConfig)
                        .scanStartTime(LocalDateTime.now())
                        .build();

                ComplianceReport report = reportGenerator.generateReport(scanContext);

                status.setState(JobState.COMPLETED);
                log.info("Job {}: Scan completed successfully", jobId);

                // Store the result
                jobResultMap.put(jobId, report);
            }

        } catch (DatabaseConnectionException e) {
            log.error("Job {}: Database connection failed", jobId, e);
            status.setState(JobState.FAILED);
            status.setErrorMessage("Database connection error: " + e.getMessage());
        } catch (MetadataExtractionException e) {
            log.error("Job {}: Metadata extraction failed", jobId, e);
            status.setState(JobState.FAILED);
            status.setErrorMessage("Metadata extraction error: " + e.getMessage());
        } catch (PiiDetectionException e) {
            log.error("Job {}: PII detection failed", jobId, e);
            status.setState(JobState.FAILED);
            status.setErrorMessage("PII detection error: " + e.getMessage());
        } catch (SQLException e) {
            log.error("Job {}: SQL error during scan", jobId, e);
            status.setState(JobState.FAILED);
            status.setErrorMessage("SQL error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Job {}: Scan failed with unexpected error", jobId, e);
            status.setState(JobState.FAILED);
            status.setErrorMessage("Unexpected error: " + e.getMessage());
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
        JobStatus status = jobStatusMap.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
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
        JobStatus status = jobStatusMap.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }

        if (status.getState() != JobState.COMPLETED) {
            throw new IllegalStateException("Scan job is not completed. Current state: " + status.getState());
        }

        ComplianceReport report = jobResultMap.get(jobId);
        if (report == null) {
            throw new IllegalStateException("Report not found for completed job: " + jobId);
        }

        return report;
    }

    /**
     * Cancels a scan job if it's still in progress.
     *
     * @param jobId The UUID of the job to cancel
     * @throws IllegalArgumentException if the job ID is not found
     * @throws IllegalStateException if the job is already completed or failed
     */
    public void cancelScan(UUID jobId) {
        JobStatus status = jobStatusMap.get(jobId);
        if (status == null) {
            throw new IllegalArgumentException("Job ID not found: " + jobId);
        }

        if (status.getState() == JobState.COMPLETED || status.getState() == JobState.FAILED) {
            throw new IllegalStateException("Cannot cancel job that is already " + status.getState());
        }

        status.setState(JobState.FAILED);
        status.setErrorMessage("Job cancelled by user request");
        log.info("Job {}: Cancelled by user request", jobId);
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
                .build();
    }
}