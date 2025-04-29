package com.privsense.api.dto.result;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    private boolean sensitiveData;
    
    // Quasi-identifier related fields
    private boolean isQuasiIdentifier;
    private double quasiIdentifierRiskScore;
    private String quasiIdentifierType;
    private List<String> correlatedColumns;
}