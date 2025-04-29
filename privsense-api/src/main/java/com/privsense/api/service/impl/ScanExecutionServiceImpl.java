package com.privsense.api.service.impl;

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
public class ScanExecutionServiceImpl implements ScanExecutionService {

    private final DatabaseConnector databaseConnector;
    private final MetadataExtractor metadataExtractor;
    private final ConsolidatedSampler sampler;
    private final PiiDetector piiDetector;
    private final ConsolidatedReportService reportGenerator;
    private final ConnectionRepository connectionRepository;
    private final PrivSenseConfigProperties configProperties;
    private final ScanPersistenceService scanPersistenceService;

    /**
     * Executes the scan job asynchronously.
     * This method is executed in a separate thread managed by Spring's TaskExecutor.
     *
     * @param jobId The UUID of the job
     * @param request The scan request details
     */
    @Override
    @Async("taskExecutor")
    public CompletableFuture<Void> executeScanAsync(UUID jobId, Object requestObj) {
        ScanRequest request = (ScanRequest) requestObj;
        
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

                // Create scan context with the required data for report generation
                LocalDateTime scanStartTime = scanInfo.getStartTime() != null ? 
                    LocalDateTime.ofInstant(scanInfo.getStartTime(), java.time.ZoneId.systemDefault()) : 
                    LocalDateTime.now();
                    
                LocalDateTime scanEndTime = LocalDateTime.now();
                
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
                scanPersistenceService.saveReport(jobId, report);

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
                        request.getDetectionConfig().getNerThreshold() : configProperties.getDetection().getNerThreshold())
                .reportingThreshold(configProperties.getDetection().getReportingThreshold())
                .stopPipelineOnHighConfidence(configProperties.getDetection().isStopPipelineOnHighConfidence())
                .entropyCalculationEnabled(configProperties.getDetection().isEntropyEnabled())
                .build();
    }
}