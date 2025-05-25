package com.privsense.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

/**
 * Data Transfer Object containing statistics about a scan.
 * Used for the enhanced report access API.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanStatsDTO extends BaseResponseDTO {
    // Basic scan information
    private String scanId;
    private String databaseName;
    private String databaseProductName;
    private String databaseProductVersion;
    private Instant startTime;
    private Instant endTime;
    private String scanDuration;
    private String status;
    
    // Statistics
    private int totalTablesScanned;
    private int totalColumnsScanned;
    private int piiColumnsFound;
    private int quasiIdentifierColumnsFound;
    private int totalPiiCandidates;
    
    // Top PII types (category -> count)
    private Map<String, Integer> piiTypeDistribution;
    
    // Success indicators
    private boolean completed;
    private boolean failed;
}