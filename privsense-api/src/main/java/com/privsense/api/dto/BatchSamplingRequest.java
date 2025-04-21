package com.privsense.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for requesting parallel sampling across multiple tables.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSamplingRequest {
    
    @NotNull(message = "Connection ID is required")
    private UUID connectionId;
    
    @NotEmpty(message = "At least one table must be specified")
    private List<@Valid TableSamplingRequest> tables;
    
    @Min(value = 1, message = "Sample size must be at least 1")
    private Integer defaultSampleSize;
    
    private String defaultSamplingMethod;
    
    private Boolean calculateEntropy;
    
    private Integer maxConcurrentTables;
    
    /**
     * Request details for a single table to sample.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSamplingRequest {
        @NotNull(message = "Table name is required")
        private String tableName;
        
        private List<String> columnNames; // If null or empty, all columns will be sampled
        
        private Integer sampleSize; // If null, defaultSampleSize will be used
    }
}