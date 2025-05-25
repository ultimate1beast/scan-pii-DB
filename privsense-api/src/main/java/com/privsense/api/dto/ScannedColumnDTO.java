package com.privsense.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a scanned database column with PII detection results.
 * Used for the enhanced report access API.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScannedColumnDTO extends BaseResponseDTO {
    // Column identification
    private UUID id;
    private UUID tableId;
    private String columnName;
    private String dataType;
    private String qualifiedName; // Fully qualified name (schema.table.column)
    
    // PII detection results
    private boolean hasPii;
    private boolean isQuasiIdentifier;
    private String highestConfidencePiiType;
    private Double confidenceScore;
    private List<String> detectionMethods;
    
    // Risk classification
    private String riskLevel; // HIGH, MEDIUM, LOW, NONE
    
    // Additional metadata
    private List<String> correlatedColumns;
    private String clusteringMethod;
    private Double entropy;
}