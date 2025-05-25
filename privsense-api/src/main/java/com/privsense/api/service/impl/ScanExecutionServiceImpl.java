package com.privsense.api.service.impl;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.api.dto.ScanRequest;
import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.exception.PiiDetectionException;
import com.privsense.core.model.*;
import com.privsense.core.repository.ConnectionRepository;
import com.privsense.core.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


/**
 * Service that handles the execution of scan jobs.
 * Coordinates the different steps involved in the scan process.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanExecutionServiceImpl implements ScanExecutionService {    private final DatabaseConnector databaseConnector;
    private final MetadataExtractor metadataExtractor;
    private final ConsolidatedSampler sampler;
    private final PiiDetector piiDetector;
    private final ConsolidatedReportService reportGenerator;
    private final ConnectionRepository connectionRepository;
    private final PrivSenseConfigProperties configProperties;
    private final ScanPersistenceService scanPersistenceService;
    private final NotificationService notificationService;
    private final WebSocketPiiDetectionProgressCallback progressCallback;

    /**
     * Executes the scan job asynchronously.
     * This method is executed in a separate thread managed by Spring's TaskExecutor.
     *
     * @param jobId The UUID of the job
     * @param requestObj The scan request details
     * @return A CompletableFuture that completes when the scan has finished
     */
    @Override
    @Async("taskExecutor")
    public CompletableFuture<Void> executeScanAsync(UUID jobId, Object requestObj) {
        if (!(requestObj instanceof ScanRequest)) {
            log.error("Job {}: Invalid request object type: {}", jobId, 
                    requestObj != null ? requestObj.getClass().getName() : "null");
            return CompletableFuture.completedFuture(null);
        }
        
        ScanRequest request = (ScanRequest) requestObj;
        Connection connection = null;
        
        try {
            // 1. Get connection ID and validate it
            UUID connectionId = request.getConnectionId();
            if (connectionId == null) {
                throw new IllegalArgumentException("Connection ID cannot be null");
            }
              // Update scan status to EXTRACTING_METADATA
            updateScanStatusAndNotify(jobId, ScanMetadata.ScanStatus.EXTRACTING_METADATA);
            log.info("Job {}: Extracting metadata from database", jobId);
            
            // Set up progress callback for real-time updates
            progressCallback.setJobId(jobId.toString());
            progressCallback.onScanPhaseChanged("METADATA_EXTRACTION", "Extracting database metadata");

            // Verify connection exists before trying to get a connection object
            if (!connectionRepository.existsById(connectionId)) {
                throw new IllegalArgumentException("Connection ID not found: " + connectionId);
            }

            // Get database connection
            connection = databaseConnector.getConnection(connectionId);
            if (connection == null) {
                throw new DatabaseConnectionException("Failed to obtain database connection");
            }
            
            // Get database information to update the scan record
            String dbName = connection.getCatalog();
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            String dbProductVersion = connection.getMetaData().getDatabaseProductVersion();
            
            // Update scan metadata with database information
            ScanMetadata scanInfo = scanPersistenceService.getScanById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Scan not found: " + jobId));
            scanInfo.setDatabaseName(dbName);
            scanInfo.setDatabaseProductName(dbProductName);
            scanInfo.setDatabaseProductVersion(dbProductVersion);
            scanPersistenceService.save(scanInfo);
            
            // 2. Extract metadata
            SchemaInfo schemaInfo = metadataExtractor.extractMetadata(connection);
            List<TableInfo> targetTables = filterTargetTables(schemaInfo, request.getTargetTables());
            
            // Track progress information
            int totalTables = targetTables.size();
            int totalColumns = targetTables.stream()
                    .mapToInt(table -> table.getColumns().size())
                    .sum();
            scanInfo.setTotalColumnsScanned(totalColumns);
            scanPersistenceService.save(scanInfo);            // Update scan status to SAMPLING
            updateScanStatusAndNotify(jobId, ScanMetadata.ScanStatus.SAMPLING);
            log.info("Job {}: Sampling data from {} columns in {} tables", jobId, totalColumns, totalTables);
            
            // Notify progress callback about sampling phase
            progressCallback.onScanPhaseChanged("DATA_SAMPLING", "Sampling data from " + totalColumns + " columns");

            // 3. Sample data
            List<ColumnInfo> columnsToSample = getAllColumns(targetTables);
            SamplingConfig samplingConfig = buildSamplingConfig(request);

            Map<ColumnInfo, SampleData> sampledData = sampler.extractSamples(
                    connection,
                    columnsToSample,
                    samplingConfig
            );            // Update scan status to DETECTING_PII
            updateScanStatusAndNotify(jobId, ScanMetadata.ScanStatus.DETECTING_PII);
            log.info("Job {}: Detecting PII in sampled data", jobId);
            
            // Notify progress callback about PII detection phase
            progressCallback.onScanPhaseChanged("PII_DETECTION", "Analyzing " + totalColumns + " columns for PII");

            // 4. Detect PII with progress callbacks
            DetectionConfig detectionConfig = buildDetectionConfig(request);
            
            // Set up progress callback for real-time updates
            progressCallback.setJobId(jobId.toString());
            
            // Use the new progress callback version of detectPii
            List<DetectionResult> detectionResults = piiDetector.detectPii(sampledData, progressCallback);
            
            // Save detection results and update statistics
            scanPersistenceService.saveScanResults(jobId, detectionResults);
            int piiColumnsFound = (int) detectionResults.stream()
                    .filter(result -> result.getConfidenceScore() >= detectionConfig.getReportingThreshold())
                    .count();
            
            scanInfo = scanPersistenceService.getScanById(jobId)
                    .orElseThrow(() -> new IllegalArgumentException("Scan not found: " + jobId));
            scanInfo.setTotalPiiColumnsFound(piiColumnsFound);
            scanPersistenceService.save(scanInfo);
              // Update scan status to GENERATING_REPORT
            updateScanStatusAndNotify(jobId, ScanMetadata.ScanStatus.GENERATING_REPORT);
            log.info("Job {}: Generating compliance report", jobId);
            
            // Notify progress callback about report generation phase
            progressCallback.onScanPhaseChanged("REPORT_GENERATION", "Generating compliance report");

            // 5. Generate report - safely close the connection before lengthy report generation
            DatabaseConnectionInfo connectionInfoForReport = connectionRepository.findById(connectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Connection ID not found for report context: " + connectionId));

            // Create scan context with the required data for report generation
            LocalDateTime scanStartTime = scanInfo.getStartTime() != null ? 
                LocalDateTime.ofInstant(scanInfo.getStartTime(), java.time.ZoneId.systemDefault()) : 
                LocalDateTime.now();
                
            LocalDateTime scanEndTime = LocalDateTime.now();
            
            // Safely close the connection before the potentially lengthy report generation
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    connection = null;
                }
            } catch (SQLException e) {
                log.warn("Job {}: Error closing database connection: {}", jobId, e.getMessage());
                // We can still continue with report generation even if connection closing failed
            }
            
            ScanContext scanContext = ScanContext.builder()
                    .scanId(jobId)
                    .databaseConnectionInfo(connectionInfoForReport)
                    .schemaInfo(schemaInfo)
                    .sampledData(sampledData)
                    .detectionResults(detectionResults)
                    .samplingConfig(samplingConfig)
                    .detectionConfig(detectionConfig)
                    .scanStartTime(scanStartTime)
                    .scanEndTime(scanEndTime)
                    .databaseProductName(dbProductName)
                    .databaseProductVersion(dbProductVersion)
                    .build();

            // Generate the report and store it in the database
            ComplianceReport report = reportGenerator.generateReport(scanContext);
            
            // Store the generated report in the database
            scanPersistenceService.saveReport(jobId, report);            // Update scan status to COMPLETED
            scanPersistenceService.completeScan(jobId);
            
            // Send final scan completion notification with results
            progressCallback.onScanCompleted(
                totalColumns, 
                piiColumnsFound, 
                0, // TODO: Add QI columns count when QI analysis is implemented
                detectionResults.stream()
                    .filter(result -> result.getConfidenceScore() >= detectionConfig.getReportingThreshold())
                    .map(DetectionResult::getPiiType)
                    .collect(java.util.stream.Collectors.toSet()),
                0, // TODO: Add QI groups count when QI analysis is implemented
                calculateComplianceScore(piiColumnsFound, totalColumns)
            );
            
            // Send WebSocket notification for COMPLETED status
            sendStatusNotification(jobId);
            
            log.info("Job {}: Scan completed successfully. Found {} PII columns out of {} total columns", 
                    jobId, piiColumnsFound, totalColumns);

        } catch (DatabaseConnectionException e) {
            handleScanError(jobId, "Database connection failed", e);
        } catch (MetadataExtractionException e) {
            handleScanError(jobId, "Metadata extraction failed", e);
        } catch (PiiDetectionException e) {
            handleScanError(jobId, "PII detection failed", e);
        } catch (SQLException e) {
            handleScanError(jobId, "SQL error during scan", e);
        } catch (IllegalArgumentException e) {
            handleScanError(jobId, "Invalid input parameter", e);
        } catch (Exception e) {
            handleScanError(jobId, "Scan failed with unexpected error", e);
        } finally {
            // Always ensure the connection is closed
            if (connection != null) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    log.warn("Job {}: Error closing database connection in finally block: {}", 
                            jobId, e.getMessage());
                }
            }
        }

        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Helper method to update scan status and send notification in one step
     */
    private void updateScanStatusAndNotify(UUID jobId, ScanMetadata.ScanStatus status) {
        scanPersistenceService.updateScanStatus(jobId, status);
        sendStatusNotification(jobId);
    }
    
    /**
     * Helper method to handle scan errors consistently
     */
    private void handleScanError(UUID jobId, String message, Exception e) {
        log.error("Job {}: {}: {}", jobId, message, e.getMessage(), e);
        String errorMsg = String.format("%s: %s", message, e.getMessage());
        scanPersistenceService.failScan(jobId, errorMsg);
        sendStatusNotification(jobId);
    }
    
    /**
     * Helper method to send status notifications via WebSocket
     */
    private void sendStatusNotification(UUID jobId) {
        try {
            // Get the current job status
            scanPersistenceService.getScanById(jobId).ifPresent(scanMetadata -> {
                try {
                    // Create a job response object that can be sent as a notification
                    ScanJobResponse jobStatus = createScanJobResponse(scanMetadata);
                    
                    // Send the notification
                    notificationService.sendScanStatusUpdate(jobStatus);
                } catch (Exception e) {
                    log.warn("Failed to send notification for job {}: {}", jobId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("Error retrieving scan data for notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to create a ScanJobResponse from ScanMetadata
     */
    private ScanJobResponse createScanJobResponse(ScanMetadata scanMetadata) {
        ScanJobResponse response = new ScanJobResponse();
        response.setJobId(scanMetadata.getId());
        response.setConnectionId(scanMetadata.getConnectionId());
        response.setStatus(scanMetadata.getStatus().name());
        response.setCurrentOperation(scanStatusToOperation(scanMetadata.getStatus().name()));
        
        // Estimate progress based on status
        response.setProgress(estimateProgressFromStatus(scanMetadata.getStatus()));
        
        // Set completion flags based on status
        response.setCompleted(isCompletedStatus(scanMetadata.getStatus()));
        response.setFailed(isFailedStatus(scanMetadata.getStatus()));
        
        // Set database information if available
        response.setDatabaseName(scanMetadata.getDatabaseName());
        response.setDatabaseProductName(scanMetadata.getDatabaseProductName());
        
        // Convert Instant to String for start time
        if (scanMetadata.getStartTime() != null) {
            LocalDateTime startTimeLocal = LocalDateTime.ofInstant(
                scanMetadata.getStartTime(), java.time.ZoneId.systemDefault());
            response.setStartTime(startTimeLocal.toString());
        }
        
        // Convert Instant to String for end time and set last update time directly
        if (scanMetadata.getEndTime() != null) {
            LocalDateTime endTimeLocal = LocalDateTime.ofInstant(
                scanMetadata.getEndTime(), java.time.ZoneId.systemDefault());
            response.setEndTime(endTimeLocal.toString());
            response.setLastUpdateTime(endTimeLocal);
        } else {
            // Use current time if no end time available
            response.setLastUpdateTime(LocalDateTime.now());
        }
        
        // Set error message if available
        response.setErrorMessage(scanMetadata.getErrorMessage());
        
        // Set PII scan results if available
        if (scanMetadata.getTotalColumnsScanned() != null) {
            response.setTotalColumnsScanned(scanMetadata.getTotalColumnsScanned());
        }
        if (scanMetadata.getTotalPiiColumnsFound() != null) {
            response.setTotalPiiColumnsFound(scanMetadata.getTotalPiiColumnsFound());
        }
        
        return response;
    }
    
    /**
     * Helper method to determine if a status represents a completed scan
     */
    private boolean isCompletedStatus(ScanMetadata.ScanStatus status) {
        return status == ScanMetadata.ScanStatus.COMPLETED;
    }
    
    /**
     * Helper method to determine if a status represents a failed scan
     */
    private boolean isFailedStatus(ScanMetadata.ScanStatus status) {
        return status == ScanMetadata.ScanStatus.FAILED || 
               status == ScanMetadata.ScanStatus.CANCELLED;
    }
    
    /**
     * Helper method to estimate progress percentage based on scan status
     */
    private Integer estimateProgressFromStatus(ScanMetadata.ScanStatus status) {
        return switch (status) {
            case PENDING -> 0;
            case EXTRACTING_METADATA -> 10;
            case SAMPLING -> 30;
            case DETECTING_PII -> 60;
            case GENERATING_REPORT -> 85;
            case COMPLETED -> 100;
            case FAILED, CANCELLED -> 100;
            default -> null;
        };
    }
    
    /**
     * Helper method to map scan status to operation description
     */
    private String scanStatusToOperation(String status) {
        if (status == null) {
            return "";
        }
        
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Waiting to start";
            case "EXTRACTING_METADATA" -> "Extracting database metadata";
            case "SAMPLING" -> "Collecting data samples";
            case "DETECTING_PII" -> "Analyzing data for PII";
            case "GENERATING_REPORT" -> "Creating compliance report";
            case "COMPLETED" -> "Scan completed";
            case "FAILED" -> "Scan failed";
            case "CANCELLED" -> "Scan cancelled";
            default -> "Unknown";
        };
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
                .toList();
    }

    /**
     * Helper method to extract all columns from a list of tables.
     */
    private List<ColumnInfo> getAllColumns(List<TableInfo> tables) {
        return tables.stream()
                .flatMap(table -> table.getColumns().stream())
                .toList();
    }

    /**
     * Helper method to build a SamplingConfig from the request and default properties.
     */
    private SamplingConfig buildSamplingConfig(ScanRequest request) {
        return SamplingConfig.builder()
                .sampleSize(request.getSamplingConfig().getSampleSize() != null ?
                        request.getSamplingConfig().getSampleSize() : configProperties.getSampling().getDefaultSize())
                .samplingMethod(request.getSamplingConfig().getSamplingMethod() != null ?
                        request.getSamplingConfig().getSamplingMethod() : configProperties.getSampling().getDefaultMethod())
                .maxConcurrentQueries(configProperties.getSampling().getMaxConcurrentDbQueries())
                .build();
    }

    /**
     * Helper method to build a DetectionConfig from the request and default properties.
     */
    private DetectionConfig buildDetectionConfig(ScanRequest request) {
        return DetectionConfig.builder()
                .heuristicThreshold(request.getDetectionConfig().getHeuristicThreshold() != null ?
                        request.getDetectionConfig().getHeuristicThreshold() : configProperties.getDetection().getHeuristicThreshold())
                .regexThreshold(request.getDetectionConfig().getRegexThreshold() != null ?
                        request.getDetectionConfig().getRegexThreshold() : configProperties.getDetection().getRegexThreshold())
                .nerThreshold(request.getDetectionConfig().getNerThreshold() != null ?
                        request.getDetectionConfig().getNerThreshold() : configProperties.getDetection().getNerThreshold())                .reportingThreshold(configProperties.getDetection().getReportingThreshold())
                .stopPipelineOnHighConfidence(configProperties.getDetection().isStopPipelineOnHighConfidence())
                .entropyCalculationEnabled(configProperties.getDetection().isEntropyEnabled())
                .build();
    }
    
    /**
     * Calculate a simple compliance score based on the ratio of PII columns found.
     * Higher percentage of PII columns results in lower compliance score.
     */
    private double calculateComplianceScore(int piiColumns, int totalColumns) {
        if (totalColumns == 0) return 100.0;
        
        double piiRatio = (double) piiColumns / totalColumns;
        // Simple compliance score calculation: 100% compliance when no PII, 0% when all columns are PII
        return Math.max(0.0, (1.0 - piiRatio) * 100.0);
    }
}