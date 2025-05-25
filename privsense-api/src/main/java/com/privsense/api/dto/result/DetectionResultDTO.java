package com.privsense.api.dto.result;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour les résultats de détection de PII.
 * Cette classe a été créée pour éviter les références circulaires dans ComplianceReportDTO.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DetectionResultDTO extends BaseResponseDTO {
    private String tableName;
    private String columnName;
    private String dataType;
    private String piiType;
    private double confidenceScore;
    private List<String> detectionMethods;
    
    // Explicit isPii field to clearly indicate if this column contains PII
    private boolean isPii;
    
    // Additional fields for categorizing the finding
    private boolean sensitiveData;
    
    // Risk score between 0-1 (higher means more risk)
    private Double riskScore;
    
    // New fields for quasi-identifier information
    private boolean isQuasiIdentifier;
    private Double quasiIdentifierRiskScore;
    @Builder.Default
    private List<String> correlatedColumns = new ArrayList<>();
    
    // Field for clustering method used for QI detection
    private String clusteringMethod;
}