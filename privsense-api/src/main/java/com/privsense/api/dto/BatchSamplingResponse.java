package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for batch sampling operation across multiple tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSamplingResponse {
    
    private int totalTablesProcessed;
    private int totalColumnsProcessed;
    private long totalExecutionTimeMs;
    private double averageTableTimeMs;
    private List<TableSamplingResult> results;
    
    /**
     * Results for a single table's sampling operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSamplingResult {
        private String tableName;
        private int columnCount;
        private long samplingTimeMs;
        private Map<String, ColumnSamplingResult> columnResults;
        private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED
        private String errorMessage; // Only populated if status is FAILED or PARTIAL_SUCCESS
    }
    
    /**
     * Results for a single column's sampling operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ColumnSamplingResult {
        private String columnName;
        private int sampleSize;
        private String samplingMethod;
        private int actualRowCount;
        private int nullCount;
        private double nullPercentage;
        private double nonNullPercentage;
        private Double entropy;
        private boolean entropyCalculated;
        private Map<String, Long> topValues; // Top N most common values and their counts
        private String status; // SUCCESS or FAILED
        private String errorMessage;
    }
    
    /**
     * Performance and execution metrics for the batch operation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        private int maxConcurrentSamplingTasks;
        private long averageSamplingTimePerColumnMs;
        private long minColumnSamplingTimeMs;
        private long maxColumnSamplingTimeMs;
        private int totalRowsProcessed;
    }
}