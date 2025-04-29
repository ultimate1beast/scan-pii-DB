package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DetectionResult;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper spécialisé pour les résultats de détection PII et rapports de conformité.
 * Utilise la configuration centralisée MapStructConfig.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class
)
public interface DetectionMapper {

    /**
     * Map ComplianceReport vers ComplianceReportDTO.
     */
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "piiFindings", target = "piiFindings")
    ComplianceReportDTO toReportDto(ComplianceReport report);
    
    /**
     * Map DetectionResult vers DetectionResultDTO.
     */
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "highestConfidencePiiType", target = "piiType")
    @Mapping(source = "highestConfidenceScore", target = "confidenceScore")
    @Mapping(source = "detectionMethods", target = "detectionMethods")
    DetectionResultDTO toDetectionResultDto(DetectionResult result);
    
    /**
     * Map une liste de DetectionResult vers une liste de DetectionResultDTO.
     */
    List<DetectionResultDTO> toDetectionResultDtoList(List<DetectionResult> results);
    
    /**
     * Map ComplianceReport.ScanSummary vers ComplianceReportDTO.ScanSummaryDTO
     */
    @Mapping(source = "tablesScanned", target = "tablesScanned")
    @Mapping(source = "columnsScanned", target = "columnsScanned")
    @Mapping(source = "piiColumnsFound", target = "piiColumnsFound")
    @Mapping(source = "totalPiiCandidates", target = "totalPiiCandidates")
    @Mapping(source = "scanDurationMillis", target = "scanDurationMillis")
    ComplianceReportDTO.ScanSummaryDTO toSummaryDto(ComplianceReport.ScanSummary summary);
}