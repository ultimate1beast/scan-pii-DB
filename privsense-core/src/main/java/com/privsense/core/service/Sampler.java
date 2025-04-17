package com.privsense.core.service;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SamplingConfig;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Interface for orchestrating data sampling from database columns.
 * Used in the scanning pipeline to extract representative data samples.
 */
public interface Sampler {

    /**
     * Extracts data samples from a list of columns using the provided connection and sampling configuration.
     *
     * @param connection The database connection to use
     * @param columns The list of columns to sample data from
     * @param config The sampling configuration parameters
     * @return A map of column info to sample data
     */
    Map<ColumnInfo, SampleData> extractSamples(Connection connection, List<ColumnInfo> columns, SamplingConfig config);
}