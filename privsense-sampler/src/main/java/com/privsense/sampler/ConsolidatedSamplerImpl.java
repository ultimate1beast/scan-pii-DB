package com.privsense.sampler;

import com.privsense.core.exception.DataSamplingException;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;

import com.privsense.core.model.SchemaInfo;
import com.privsense.core.service.ConsolidatedSampler;
import com.privsense.sampler.factory.SamplingStrategyFactory;
import com.privsense.sampler.strategy.DbSpecificSamplingStrategy;
import com.privsense.sampler.util.EntropyCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Implementation of ConsolidatedSampler that performs parallel sampling of columns
 * with database-specific optimizations and concurrency control.
 * 
 * This class combines the functionality from both ParallelSamplerImpl and SamplerAdapter.
 */
@Service
public class ConsolidatedSamplerImpl implements ConsolidatedSampler {

    private static final Logger logger = LoggerFactory.getLogger(ConsolidatedSamplerImpl.class);

    private final com.privsense.sampler.config.SamplingConfig samplerConfig;
    private final SamplingStrategyFactory strategyFactory;
    private final EntropyCalculator entropyCalculator;
    private final ExecutorService executorService;
    private final Semaphore dbQuerySemaphore;

    /**
     * Creates a new ConsolidatedSamplerImpl with the provided dependencies.
     *
     * @param samplerConfig The configuration for sampling
     * @param strategyFactory The factory for database-specific strategies
     * @param entropyCalculator The calculator for entropy values
     */
    @Autowired
    public ConsolidatedSamplerImpl(
            com.privsense.sampler.config.SamplingConfig samplerConfig,
            SamplingStrategyFactory strategyFactory,
            EntropyCalculator entropyCalculator) {
        this.samplerConfig = samplerConfig;
        this.strategyFactory = strategyFactory;
        this.entropyCalculator = entropyCalculator;
        
        // Create an executor service with a fixed thread pool size
        // This could be configured externally for different environments
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        this.executorService = Executors.newFixedThreadPool(corePoolSize);
        
        // Create a semaphore to limit concurrent database queries
        this.dbQuerySemaphore = new Semaphore(samplerConfig.getMaxConcurrentDbQueries());
        
        logger.info("Initialized ConsolidatedSampler with thread pool size: {} and max concurrent DB queries: {}", 
                corePoolSize, samplerConfig.getMaxConcurrentDbQueries());
    }

    @Override
    public SampleData sampleColumn(Connection connection, ColumnInfo column, int sampleSize) {
        try {
            // Create a SampleData object for the column
            SampleData sampleData = new SampleData();
            sampleData.setColumnInfo(column);
            
            // Get the appropriate sampling strategy for this database
            String databaseName = connection.getMetaData().getDatabaseProductName();
            DbSpecificSamplingStrategy strategy = strategyFactory.getStrategy(databaseName);
            
            // Build the database-specific sampling query
            String query = strategy.buildSamplingQuery(column, sampleSize);
            logger.debug("Sampling query for {}.{}: {}", 
                    column.getTable().getTableName(), column.getColumnName(), query);
            
            // Execute the query
            try {
                // Acquire a permit from the semaphore to limit concurrent DB queries
                dbQuerySemaphore.acquire();
                
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    // Set fetch size for efficiency if supported
                    stmt.setFetchSize(Math.min(sampleSize, 1000));
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        // Process the result set
                        while (rs.next()) {
                            // Get the value appropriate for the column's type
                            Object value = rs.getObject(1);
                            sampleData.addSample(value);
                        }
                    }
                }
            } finally {
                // Always release the semaphore permit in a finally block
                dbQuerySemaphore.release();
            }
            
            // Calculate entropy if required by configuration
            if (samplerConfig.isEntropyCalculationEnabled()) {
                double entropy = entropyCalculator.calculateEntropy(sampleData);
                sampleData.setEntropy(entropy);
            }
            
            return sampleData;
        } catch (SQLException | InterruptedException e) {
            throw new DataSamplingException("Failed to sample column: " + column.getColumnName(), e);
        }
    }

    @Override
    public List<SampleData> sampleColumns(Connection connection, List<ColumnInfo> columns, int sampleSize) {
        try {
            // Submit tasks for each column to the executor service
            List<Future<SampleData>> futures = columns.stream()
                    .map(column -> executorService.submit(() -> sampleColumn(connection, column, sampleSize)))
                    .collect(Collectors.toList());
            
            // Collect the results from all futures
            List<SampleData> results = new ArrayList<>(columns.size());
            for (Future<SampleData> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof DataSamplingException) {
                        throw (DataSamplingException) e.getCause();
                    }
                    throw new DataSamplingException("Failed during parallel sampling", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new DataSamplingException("Sampling was interrupted", e);
                }
            }
            
            return results;
        } catch (DataSamplingException e) {
            throw e;
        } catch (Exception e) {
            throw new DataSamplingException("Unexpected error during parallel sampling", e);
        }
    }

    @Override
    public Map<ColumnInfo, SampleData> sampleSchema(Connection connection, SchemaInfo schema, int sampleSize) {
        // Extract all columns from all tables in the schema
        List<ColumnInfo> allColumns = new ArrayList<>();
        schema.getTables().forEach(table -> allColumns.addAll(table.getColumns()));
        
        // Sample all columns
        List<SampleData> samples = sampleColumns(connection, allColumns, sampleSize);
        
        // Convert list to map
        Map<ColumnInfo, SampleData> sampleMap = new HashMap<>(samples.size());
        for (SampleData sample : samples) {
            sampleMap.put(sample.getColumnInfo(), sample);
        }
        
        return sampleMap;
    }

    @Override
    public int determineOptimalSampleSize(Connection connection, ColumnInfo column) {
        // This is a simple implementation that returns the configured sample size
        // A more advanced implementation could analyze the column statistics
        // to determine an appropriate sample size dynamically
        
        try {
            // Get the table size
            String tableName = column.getTable().getTableName();
            String query = "SELECT COUNT(*) FROM " + tableName;
            
            try (PreparedStatement stmt = connection.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long rowCount = rs.getLong(1);
                    
                    // If table is very small, sample all rows
                    if (rowCount <= samplerConfig.getSampleSize()) {
                        return (int) rowCount;
                    }
                    
                    // For larger tables, use the configured sample size
                    // A more sophisticated approach could scale this based on distribution
                    return samplerConfig.getSampleSize();
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to determine optimal sample size for {}.{}: {}",
                    column.getTable().getTableName(), column.getColumnName(), e.getMessage());
        }
        
        // Default to the configured sample size if we can't determine dynamically
        return samplerConfig.getSampleSize();
    }

    @Override
    public Map<ColumnInfo, SampleData> extractSamples(Connection connection, List<ColumnInfo> columns, com.privsense.core.model.SamplingConfig config) {
        logger.debug("Extracting samples for {} columns with sample size {}", 
                columns.size(), config.getSampleSize());
        
        // Use the sampleColumns method directly
        List<SampleData> sampledData = sampleColumns(
                connection,
                columns,
                config.getSampleSize()
        );
        
        // Convert the list of SampleData to the required Map<ColumnInfo, SampleData>
        Map<ColumnInfo, SampleData> sampleMap = new HashMap<>();
        for (SampleData sample : sampledData) {
            sampleMap.put(sample.getColumnInfo(), sample);
        }
        
        return sampleMap;
    }

    /**
     * Clean up the executor service when this bean is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down executor service for ConsolidatedSampler");
        
        executorService.shutdown();
        try {
            // Wait for tasks to complete
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}