package com.privsense.core.service;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SamplingConfig;
import com.privsense.core.model.SchemaInfo;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Consolidated interface for database column sampling operations.
 * Combines functionality from both DataSampler and Sampler interfaces.
 */
public interface ConsolidatedSampler {
    
    /**
     * Samples data from a specific column in the database.
     * 
     * @param connection The active database connection
     * @param column The column to sample data from
     * @param sampleSize The number of records to sample
     * @return A SampleData object containing the sampled values
     * @throws com.privsense.core.exception.DataSamplingException if sampling fails
     */
    SampleData sampleColumn(Connection connection, ColumnInfo column, int sampleSize);
    
    /**
     * Samples data from multiple columns in the database.
     * 
     * @param connection The active database connection
     * @param columns The list of columns to sample data from
     * @param sampleSize The number of records to sample per column
     * @return A list of SampleData objects, one for each input column
     * @throws com.privsense.core.exception.DataSamplingException if sampling fails
     */
    List<SampleData> sampleColumns(Connection connection, List<ColumnInfo> columns, int sampleSize);
    
    /**
     * Samples data from all columns in the provided schema.
     * 
     * @param connection The active database connection
     * @param schema The schema containing tables and columns to sample
     * @param sampleSize The number of records to sample per column
     * @return A map with ColumnInfo as keys and corresponding SampleData as values
     * @throws com.privsense.core.exception.DataSamplingException if sampling fails
     */
    Map<ColumnInfo, SampleData> sampleSchema(Connection connection, SchemaInfo schema, int sampleSize);
    
    /**
     * Determines the optimal sample size for a given column based on its statistics.
     * 
     * @param connection The active database connection
     * @param column The column to analyze
     * @return The recommended sample size
     */
    int determineOptimalSampleSize(Connection connection, ColumnInfo column);
    
    /**
     * Extracts data samples from a list of columns using the provided connection and sampling configuration.
     * This method incorporates the configuration-based approach from the original Sampler interface.
     *
     * @param connection The database connection to use
     * @param columns The list of columns to sample data from
     * @param config The sampling configuration parameters
     * @return A map of column info to sample data
     */
    Map<ColumnInfo, SampleData> extractSamples(Connection connection, List<ColumnInfo> columns, SamplingConfig config);
}