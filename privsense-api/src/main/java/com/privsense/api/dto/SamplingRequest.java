package com.privsense.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for requesting a test sampling of a database column.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingRequest {
    
    @NotNull(message = "Connection ID is required")
    private UUID connectionId;
    
    @NotNull(message = "Table name is required")
    private String tableName;
    
    @NotNull(message = "Column name is required")
    private String columnName;
    
    @Min(value = 1, message = "Sample size must be at least 1")
    private Integer sampleSize;
    
    private String samplingMethod;
    
    private Boolean calculateEntropy;
}