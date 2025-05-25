package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.PiiCandidate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Mapping(target = "tableFindings", expression = "java(createTableFindings(report))")
    ComplianceReportDTO toReportDto(ComplianceReport report);
    
    /**
     * Ajoute les métadonnées après le mapping.
     */
    @AfterMapping
    default void addReportMetadata(@MappingTarget ComplianceReportDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
    /**
     * Map DetectionResult vers DetectionResultDTO.
     */
    @Mapping(source = "highestConfidencePiiType", target = "piiType")
    @Mapping(source = "highestConfidenceScore", target = "confidenceScore")
    @Mapping(target = "detectionMethods", expression = "java(getDetectionMethodsFromCandidates(result))")
    @Mapping(source = "quasiIdentifier", target = "isQuasiIdentifier")
    @Mapping(source = "quasiIdentifierRiskScore", target = "quasiIdentifierRiskScore")
    @Mapping(source = "correlatedColumns", target = "correlatedColumns")
    @Mapping(source = "clusteringMethod", target = "clusteringMethod")
    DetectionResultDTO toDetectionResultDto(DetectionResult result);
    
    /**
     * Ajoute les métadonnées après le mapping.
     */
    @AfterMapping
    default void addResultMetadata(@MappingTarget DetectionResultDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
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
    ComplianceReportDTO.ScanSummaryDTO toScanSummaryDto(ComplianceReport.ScanSummary summary);

    /**
     * Gets the detection methods from the candidates list
     */
    default List<String> getDetectionMethodsFromCandidates(DetectionResult detectionResult) {
        if (detectionResult == null || detectionResult.getCandidates() == null) {
            return new ArrayList<>();
        }
        
        return detectionResult.getCandidates().stream()
                .map(PiiCandidate::getDetectionMethod)
                .distinct()
                .toList();
    }

    /**
     * Creates the table findings section organized by table
     */
    default Map<String, ComplianceReportDTO.TableFindingsDTO> createTableFindings(ComplianceReport report) {
        if (report == null || report.getDetectionResults() == null) {
            return new HashMap<>();
        }
        
        // Group detection results by table name
        Map<String, List<DetectionResult>> resultsByTable = report.getDetectionResults().stream()
                .collect(Collectors.groupingBy(result -> 
                    result.getColumnInfo().getTable().getTableName()));
        
        // Convert to TableFindingsDTO for each table
        Map<String, ComplianceReportDTO.TableFindingsDTO> tableFindings = new HashMap<>();
        resultsByTable.forEach((tableName, results) -> {
            ComplianceReportDTO.TableFindingsDTO tableFinding = ComplianceReportDTO.TableFindingsDTO.builder()
                    .columns(results.stream()
                            .map(this::toDetectionResultDto)
                            .toList())
                    .build();
            tableFindings.put(tableName, tableFinding);
        });
        
        return tableFindings;
    }
}