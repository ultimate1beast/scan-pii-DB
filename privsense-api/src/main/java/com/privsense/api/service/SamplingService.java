package com.privsense.api.service;

import com.privsense.api.config.SamplingConfigProperties;
import com.privsense.api.dto.BatchSamplingRequest;
import com.privsense.api.dto.BatchSamplingResponse;
import com.privsense.api.dto.SamplingRequest;
import com.privsense.api.dto.SamplingResponse;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.mapper.DtoMapper;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SamplingConfig;
import com.privsense.core.model.TableInfo;

import com.privsense.core.service.DataSampler;
import com.privsense.core.service.DatabaseConnector;
import com.privsense.core.service.MetadataExtractor;

import com.privsense.sampler.config.SamplingConfig.SamplingMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service for performing test sampling operations on database columns.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SamplingService {

    private final DatabaseConnector databaseConnector;
    private final DataSampler dataSampler;
    private final SamplingConfigProperties samplingConfigProps;
    private final DtoMapper dtoMapper;
    private final MetadataExtractor metadataExtractor;
    

    /**
     * Performs a test sampling operation on a database column.
     * 
     * @param request The sampling request parameters
     * @return The sampling response with results and statistics
     */
    public SamplingResponse performSampling(SamplingRequest request) {
        log.info("Performing test sampling for column {}.{}", request.getTableName(), request.getColumnName());
        
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = databaseConnector.getConnection(request.getConnectionId())) {
            // Create table and column info objects
            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(request.getTableName());
            
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setColumnName(request.getColumnName());
            columnInfo.setTable(tableInfo);
            
            // Set up sampling configuration using DTO mapper
            SamplingConfig config = dtoMapper.toSamplingConfig(request);
            
            // Apply defaults if not provided in the request
            if (config.getSampleSize() <= 0) {
                config.setSampleSize(samplingConfigProps.getDefaultSize());
            }
            
            if (config.getSamplingMethod() == null) {
                config.setSamplingMethod(samplingConfigProps.getMethods().getDefault());
            }
            
            // If entropy calculation wasn't specified in the request, use the default from config
            if (request.getCalculateEntropy() == null) {
                config.setEntropyCalculationEnabled(samplingConfigProps.isEntropyCalculationEnabled());
            }
            
            // Perform the sampling
            SampleData sampleData = dataSampler.sampleColumn(connection, columnInfo, config.getSampleSize());
            
            // Capture database product info
            String dbType = connection.getMetaData().getDatabaseProductName();
            log.info("Sampled {} rows from {}.{} in {} database", 
                sampleData.getSamples().size(), 
                request.getTableName(), 
                request.getColumnName(),
                dbType);
            
            // Build response with the sampling results using DTO mapper and enhance with additional info
            SamplingResponse response = dtoMapper.toSamplingResponse(sampleData, config);
            
            // Set fields that weren't mapped automatically
            response.setTableName(request.getTableName());
            response.setColumnName(request.getColumnName());
            response.setSamplingTimeMs(System.currentTimeMillis() - startTime);
            
            // Convert value distribution to a more API-friendly format
            Map<String, Long> valueDistribution = new HashMap<>();
            Map<Object, Long> origDist = sampleData.getValueDistribution();
            
            for (Map.Entry<Object, Long> entry : origDist.entrySet()) {
                // Convert keys to strings for JSON serialization
                String key = entry.getKey() != null ? entry.getKey().toString() : "null";
                valueDistribution.put(key, entry.getValue());
            }
            response.setValueDistribution(valueDistribution);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error during sampling operation", e);
            throw new ResourceNotFoundException("Failed to perform sampling: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets the current sampling configuration information.
     * 
     * @return The configuration information
     */
    public SamplingResponse.ConfigurationInfo getSamplingConfiguration() {
        return SamplingResponse.ConfigurationInfo.builder()
            .defaultSampleSize(samplingConfigProps.getDefaultSize())
            .maxConcurrentQueries(samplingConfigProps.getMaxConcurrentDbQueries())
            .availableSamplingMethods(Arrays.stream(SamplingMethod.values())
                .map(Enum::name)
                .collect(Collectors.toList()))
            .defaultSamplingMethod(samplingConfigProps.getMethods().getDefault())
            .build();
    }

    /**
     * Performs a batch sampling operation across multiple tables and columns.
     * Uses parallel execution for faster processing.
     *
     * @param request The batch sampling request
     * @return The batch sampling response with results for all tables and columns
     */
    public BatchSamplingResponse performBatchSampling(BatchSamplingRequest request) {
        log.info("Starting batch sampling operation for {} tables", request.getTables().size());
        long startTime = System.currentTimeMillis();
        
        // Create sampling config from request using the mapper
        SamplingConfig defaultConfig = dtoMapper.toSamplingConfig(request);
        
        // Apply defaults if not provided in the request
        if (defaultConfig.getSampleSize() <= 0) {
            defaultConfig.setSampleSize(samplingConfigProps.getDefaultSize());
        }
        
        if (defaultConfig.getSamplingMethod() == null) {
            defaultConfig.setSamplingMethod(samplingConfigProps.getMethods().getDefault());
        }
                
        int maxConcurrentTables = request.getMaxConcurrentTables() != null ?
                request.getMaxConcurrentTables() : samplingConfigProps.getMaxConcurrentDbQueries();
        
        List<BatchSamplingResponse.TableSamplingResult> tableResults = new ArrayList<>();
        int totalColumnsProcessed = 0;
        
        try (Connection connection = databaseConnector.getConnection(request.getConnectionId())) {
            // Create thread pool for parallel sampling
            ExecutorService executor = Executors.newFixedThreadPool(maxConcurrentTables);
            List<Future<BatchSamplingResponse.TableSamplingResult>> futures = new ArrayList<>();
            
            // Submit sampling tasks for each table
            for (BatchSamplingRequest.TableSamplingRequest tableRequest : request.getTables()) {
                futures.add(executor.submit(() -> {
                    return sampleTable(
                        connection, 
                        tableRequest, 
                        defaultConfig
                    );
                }));
            }
            
            // Collect all results
            for (Future<BatchSamplingResponse.TableSamplingResult> future : futures) {
                try {
                    BatchSamplingResponse.TableSamplingResult result = future.get();
                    tableResults.add(result);
                    totalColumnsProcessed += result.getColumnCount();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error processing table sampling task", e);
                    // Add error result
                    tableResults.add(BatchSamplingResponse.TableSamplingResult.builder()
                        .tableName("Unknown")
                        .status("FAILED")
                        .errorMessage("Task execution failed: " + e.getMessage())
                        .columnCount(0)
                        .columnResults(Map.of())
                        .build());
                }
            }
            
            // Shutdown the executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } catch (SQLException e) {
            log.error("Error establishing database connection", e);
            throw new ResourceNotFoundException("Failed to connect to database: " + e.getMessage(), e);
        }
        
        long totalExecutionTime = System.currentTimeMillis() - startTime;
        double avgTableTime = tableResults.stream()
                .mapToLong(BatchSamplingResponse.TableSamplingResult::getSamplingTimeMs)
                .average()
                .orElse(0);
                
        // Build the response
        return BatchSamplingResponse.builder()
                .totalTablesProcessed(tableResults.size())
                .totalColumnsProcessed(totalColumnsProcessed)
                .totalExecutionTimeMs(totalExecutionTime)
                .averageTableTimeMs(avgTableTime)
                .results(tableResults)
                .build();
    }
    
    /**
     * Samples a single table with specified columns.
     */
    private BatchSamplingResponse.TableSamplingResult sampleTable(
            Connection connection,
            BatchSamplingRequest.TableSamplingRequest tableRequest,
            SamplingConfig defaultConfig) {
            
        log.info("Sampling table: {}", tableRequest.getTableName());
        long tableStartTime = System.currentTimeMillis();
        
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableRequest.getTableName());
        
        Map<String, BatchSamplingResponse.ColumnSamplingResult> columnResults = new HashMap<>();
        List<String> columns = tableRequest.getColumnNames();
        
        try {
            // If no columns specified, get all columns from the table
            if (columns == null || columns.isEmpty()) {
                columns = metadataExtractor.getTableColumns(connection, tableRequest.getTableName());
            }
            
            // Configure sampling with table-specific sample size if provided
            SamplingConfig config = SamplingConfig.builder()
                    .sampleSize(tableRequest.getSampleSize() != null ? 
                        tableRequest.getSampleSize() : defaultConfig.getSampleSize())
                    .samplingMethod(defaultConfig.getSamplingMethod())
                    .entropyCalculationEnabled(defaultConfig.getEntropyCalculationEnabled())
                    .build();
            
            // Sample each column
            for (String columnName : columns) {
                try {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setColumnName(columnName);
                    columnInfo.setTable(tableInfo);
                    
                    SampleData sampleData = dataSampler.sampleColumn(connection, columnInfo, config.getSampleSize());
                    
                    // Use mapper to create the column result
                    BatchSamplingResponse.ColumnSamplingResult columnResult = 
                            dtoMapper.toColumnSamplingResult(sampleData, columnInfo, config);
                            
                    // Add the top values which aren't mapped automatically
                    columnResult.setTopValues(getTopValues(sampleData.getValueDistribution(), 5));
                            
                    columnResults.put(columnName, columnResult);
                    
                } catch (Exception e) {
                    log.error("Error sampling column {}.{}", tableRequest.getTableName(), columnName, e);
                    
                    // Add error result for this column
                    BatchSamplingResponse.ColumnSamplingResult errorResult = BatchSamplingResponse.ColumnSamplingResult.builder()
                            .columnName(columnName)
                            .status("FAILED")
                            .errorMessage(e.getMessage())
                            .build();
                            
                    columnResults.put(columnName, errorResult);
                }
            }
            
            // Calculate table status based on column results
            String status = "SUCCESS";
            String errorMessage = null;
            
            long failedColumns = columnResults.values().stream()
                    .filter(r -> "FAILED".equals(r.getStatus()))
                    .count();
                    
            if (failedColumns == columnResults.size()) {
                status = "FAILED";
                errorMessage = "All columns failed to sample";
            } else if (failedColumns > 0) {
                status = "PARTIAL_SUCCESS";
                errorMessage = failedColumns + " out of " + columnResults.size() + " columns failed to sample";
            }
            
            long tableSamplingTime = System.currentTimeMillis() - tableStartTime;
            
            return BatchSamplingResponse.TableSamplingResult.builder()
                    .tableName(tableRequest.getTableName())
                    .columnCount(columnResults.size())
                    .columnResults(columnResults)
                    .samplingTimeMs(tableSamplingTime)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error sampling table {}", tableRequest.getTableName(), e);
            
            return BatchSamplingResponse.TableSamplingResult.builder()
                    .tableName(tableRequest.getTableName())
                    .status("FAILED")
                    .errorMessage("Table sampling failed: " + e.getMessage())
                    .columnCount(0)
                    .columnResults(Map.of())
                    .samplingTimeMs(System.currentTimeMillis() - tableStartTime)
                    .build();
        }
    }
    
    /**
     * Extracts the top N most frequent values from the distribution.
     */
    private Map<String, Long> getTopValues(Map<Object, Long> distribution, int limit) {
        return distribution.entrySet().stream()
                .sorted(Map.Entry.<Object, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                    e -> e.getKey() != null ? e.getKey().toString() : "null",
                    Map.Entry::getValue,
                    (v1, v2) -> v1,
                    LinkedHashMap::new
                ));
    }
}