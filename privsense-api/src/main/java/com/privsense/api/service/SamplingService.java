package com.privsense.api.service;

import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.api.dto.BatchSamplingRequest;
import com.privsense.api.dto.BatchSamplingResponse;
import com.privsense.api.dto.SamplingRequest;
import com.privsense.api.dto.SamplingResponse;
import com.privsense.api.dto.config.SamplingConfigDTO;
import com.privsense.api.dto.result.ColumnSamplingResult;
import com.privsense.api.dto.result.TableSamplingResult;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.mapper.DtoMapper;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SamplingConfig;
import com.privsense.core.model.TableInfo;

import com.privsense.core.service.ConsolidatedSampler;
import com.privsense.core.service.DatabaseConnector;
import com.privsense.core.service.MetadataExtractor;

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
    private final ConsolidatedSampler sampler;
    private final PrivSenseConfigProperties configProperties;
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
                config.setSampleSize(configProperties.getSampling().getDefaultSize());
            }
            
            if (config.getSamplingMethod() == null) {
                config.setSamplingMethod(configProperties.getSampling().getDefaultMethod());
            }
            
            // If entropy calculation wasn't specified in the request, use the default from config
            if (request.getConfig().getEntropyCalculationEnabled() == null) {
                config.setEntropyCalculationEnabled(configProperties.getSampling().isEntropyCalculationEnabled());
            }
            
            // Perform the sampling
            SampleData sampleData = sampler.sampleColumn(connection, columnInfo, config.getSampleSize());
            
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
    public SamplingConfigDTO getSamplingConfiguration() {
        return SamplingConfigDTO.builder()
            .sampleSize(configProperties.getSampling().getDefaultSize())
            .maxConcurrentQueries(configProperties.getSampling().getMaxConcurrentDbQueries())
            .samplingMethod(configProperties.getSampling().getDefaultMethod())
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
            defaultConfig.setSampleSize(configProperties.getSampling().getDefaultSize());
        }
        
        if (defaultConfig.getSamplingMethod() == null) {
            defaultConfig.setSamplingMethod(configProperties.getSampling().getDefaultMethod());
        }
                
        int maxConcurrentTables = configProperties.getSampling().getMaxConcurrentDbQueries();
        
        List<TableSamplingResult> tableResults = new ArrayList<>();
        int totalColumnsProcessed = 0;
        
        try (Connection connection = databaseConnector.getConnection(request.getConnectionId())) {
            // Create thread pool for parallel sampling
            ExecutorService executor = Executors.newFixedThreadPool(maxConcurrentTables);
            List<Future<TableSamplingResult>> futures = new ArrayList<>();
            
            // Submit sampling tasks for each table
            for (BatchSamplingRequest.TableSamplingRequest tableRequest : request.getTables()) {
                futures.add(executor.submit(() -> 
                    sampleTableForBatch(connection, tableRequest, defaultConfig)
                ));
            }
            
            // Collect all results
            for (Future<TableSamplingResult> future : futures) {
                try {
                    TableSamplingResult result = future.get();
                    tableResults.add(result);
                    totalColumnsProcessed += result.getColumnCount();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error processing table sampling task", e);
                    // Add error result
                    tableResults.add(TableSamplingResult.builder()
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
                .mapToLong(TableSamplingResult::getSamplingTimeMs)
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
    private TableSamplingResult sampleTableForBatch(
            Connection connection,
            BatchSamplingRequest.TableSamplingRequest tableRequest,
            SamplingConfig defaultConfig) {
            
        log.info("Sampling table: {}", tableRequest.getTableName());
        long tableStartTime = System.currentTimeMillis();
        
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableRequest.getTableName());
        
        Map<String, ColumnSamplingResult> columnResults = new HashMap<>();
        Set<String> columnNames = tableRequest.getColumnNames();
        
        try {
            // If no columns specified, get all columns from the table
            List<String> columns = new ArrayList<>();
            if (columnNames == null || columnNames.isEmpty()) {
                columns = metadataExtractor.getTableColumns(connection, tableRequest.getTableName());
            } else {
                columns.addAll(columnNames);
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
                    
                    SampleData sampleData = sampler.sampleColumn(connection, columnInfo, config.getSampleSize());
                    
                    // Use mapper to create the column result
                    ColumnSamplingResult columnResult = 
                            dtoMapper.toColumnSamplingResult(sampleData, columnInfo, config);
                            
                    // Add the top values which aren't mapped automatically
                    columnResult.setTopValues(getTopValues(sampleData.getValueDistribution(), 5));
                            
                    columnResults.put(columnName, columnResult);
                    
                } catch (Exception e) {
                    log.error("Error sampling column {}.{}", tableRequest.getTableName(), columnName, e);
                    
                    // Add error result for this column
                    ColumnSamplingResult errorResult = ColumnSamplingResult.builder()
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
            
            return TableSamplingResult.builder()
                    .tableName(tableRequest.getTableName())
                    .columnCount(columnResults.size())
                    .columnResults(columnResults)
                    .samplingTimeMs(tableSamplingTime)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error sampling table {}", tableRequest.getTableName(), e);
            
            return TableSamplingResult.builder()
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