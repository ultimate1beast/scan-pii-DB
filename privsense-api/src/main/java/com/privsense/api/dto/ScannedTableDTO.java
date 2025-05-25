package com.privsense.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object representing a scanned database table with PII statistics.
 * Used for the enhanced report access API.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScannedTableDTO extends BaseResponseDTO {
    // Table identification
    private UUID id;
    private String schemaName;
    private String tableName;
    private String qualifiedName; // Fully qualified name (schema.table)
    
    // Statistics
    private int totalColumns;
    private int piiColumnCount;
    private int quasiIdentifierColumnCount;
    private int highRiskColumnCount;
    private int mediumRiskColumnCount;
    private int lowRiskColumnCount;
    
    // PII type distribution in this table (pii type -> count)
    private Map<String, Integer> piiTypeDistribution;
}