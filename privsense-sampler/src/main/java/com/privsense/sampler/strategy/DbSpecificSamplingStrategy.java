package com.privsense.sampler.strategy;

import com.privsense.core.model.ColumnInfo;

/**
 * Strategy interface for database-specific sampling queries.
 * Different databases might have different optimal ways to sample data randomly.
 */
public interface DbSpecificSamplingStrategy {

    /**
     * Constructs the database-specific SQL query for sampling data from a column.
     * 
     * @param columnInfo The column to sample from
     * @param sampleSize The number of rows to sample
     * @return The SQL query string
     */
    String buildSamplingQuery(ColumnInfo columnInfo, int sampleSize);
    
    /**
     * Returns the database product name this strategy supports
     * 
     * @return The database product name (e.g., "MySQL", "PostgreSQL")
     */
    String getSupportedDatabaseName();
}