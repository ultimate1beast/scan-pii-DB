package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.SchemaInfoDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SchemaInfo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper pour la conversion entre entités de domaine et DTOs.
 * Utilise MapStruct à la place de ModelMapper pour de meilleures performances.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class,
    builder = @org.mapstruct.Builder(disableBuilder = false)
)
public interface EntityMapper {
    
    /**
     * Convertit SchemaInfo en SchemaInfoDTO
     */
    @Mapping(source = "tables", target = "tables")
    @Mapping(target = "totalTableCount", expression = "java(schemaInfo.getTables() != null ? schemaInfo.getTables().size() : 0)")
    @Mapping(target = "totalColumnCount", expression = "java(schemaInfo.getTotalColumnCount())")
    @Mapping(target = "totalRelationshipCount", expression = "java(countRelationships(schemaInfo))")
    SchemaInfoDTO toSchemaDto(SchemaInfo schemaInfo);
    
    /**
     * Add metadata to SchemaInfoDTO after mapping
     */
    @AfterMapping
    default void addSchemaMetadata(@MappingTarget SchemaInfoDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
    /**
     * Convertit DatabaseConnectionInfo en ConnectionResponse
     */
    @Mapping(source = "id", target = "connectionId")
    @Mapping(target = "databaseProductName", ignore = true)
    @Mapping(target = "databaseProductVersion", ignore = true)
    @Mapping(target = "status", constant = "UNKNOWN")
    com.privsense.api.dto.ConnectionResponse toConnectionResponse(DatabaseConnectionInfo connectionInfo);

    /**
     * Convertit ComplianceReport vers ComplianceReportDTO avec la nouvelle structure imbriquée
     */
    @Mapping(target = "scanInfo", expression = "java(createScanInfo(report))") 
    @Mapping(target = "databaseInfo", expression = "java(createDatabaseInfo(report))")
    @Mapping(target = "scanSummary", expression = "java(createScanSummary(report))")
    @Mapping(target = "scanConfiguration", expression = "java(createScanConfiguration(report))")
    @Mapping(target = "tableFindings", expression = "java(createTableFindings(report))")
    @Mapping(target = "piiTypesDetected", expression = "java(createPiiTypesDetected(report))")
    @Mapping(target = "detectionMethodsSummary", expression = "java(createDetectionMethodsSummary(report))")
    ComplianceReportDTO toDto(ComplianceReport report);
    
    /**
     * Add metadata to ComplianceReportDTO after mapping
     */
    @AfterMapping
    default void addComplianceReportMetadata(@MappingTarget ComplianceReportDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
    /**
     * Convertit DetectionResult vers DetectionResultDTO
     */
    @Mapping(source = "highestConfidencePiiType", target = "piiType")
    @Mapping(source = "highestConfidenceScore", target = "confidenceScore", qualifiedByName = "roundToTwoDecimalPlaces")
    @Mapping(source = "pii", target = "isPii")
    @Mapping(target = "riskScore", expression = "java(roundToTwoDecimalPlaces(calculateRiskScore(detectionResult)))")
    @Mapping(target = "detectionMethods", expression = "java(getDetectionMethodsFromCandidates(detectionResult))")
    @Mapping(source = "columnInfo.columnName", target = "columnName")
    @Mapping(source = "columnInfo.table.tableName", target = "tableName")
    @Mapping(source = "columnInfo.databaseTypeName", target = "dataType")
    @Mapping(source = "quasiIdentifier", target = "isQuasiIdentifier")
    @Mapping(source = "quasiIdentifierRiskScore", target = "quasiIdentifierRiskScore", qualifiedByName = "roundToTwoDecimalPlaces")
    @Mapping(source = "correlatedColumns", target = "correlatedColumns")
    @Mapping(source = "clusteringMethod", target = "clusteringMethod")
    @Mapping(target = "sensitiveData", expression = "java(detectionResult.hasPii())")
    DetectionResultDTO toDto(DetectionResult detectionResult);
    
    /**
     * Add metadata to DetectionResultDTO after mapping
     */
    @AfterMapping
    default void addDetectionResultMetadata(@MappingTarget DetectionResultDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
    /**
     * Calculate a combined risk score based on confidence and other factors
     */
    default Double calculateRiskScore(DetectionResult detectionResult) {
        double baseScore = detectionResult.getHighestConfidenceScore();
        
        // Increase score for direct identifiers
        if (isDirectIdentifier(detectionResult.getHighestConfidencePiiType())) {
            baseScore = Math.min(1.0, baseScore * 1.2);
        }
        
        // Quasi-identifiers have their own risk score
        if (detectionResult.isQuasiIdentifier() && detectionResult.getQuasiIdentifierRiskScore() != null) {
            return detectionResult.getQuasiIdentifierRiskScore();
        }
        
        return baseScore;
    }
    
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
     * Creates the scan information section
     */
    default ComplianceReportDTO.ScanInfoDTO createScanInfo(ComplianceReport report) {
        if (report == null) {
            return null;
        }
        
        String durationStr = "";
        if (report.getScanDuration() != null) {
            durationStr = report.getScanDuration().toMillis() + "ms";
        }
        
        return ComplianceReportDTO.ScanInfoDTO.builder()
                .scanId(report.getScanId() != null ? report.getScanId().toString() : null)
                .reportId(report.getReportId())
                .scanStartTime(report.getScanStartTime())
                .scanEndTime(report.getScanEndTime())
                .scanDuration(durationStr)
                .build();
    }
    
    /**
     * Creates the database information section
     */
    default ComplianceReportDTO.DatabaseInfoDTO createDatabaseInfo(ComplianceReport report) {
        if (report == null) {
            return null;
        }
        
        return ComplianceReportDTO.DatabaseInfoDTO.builder()
                .host(report.getDatabaseHost())
                .name(report.getDatabaseName())
                .product(report.getDatabaseProductName())
                .version(report.getDatabaseProductVersion())
                .build();
    }
    
    /**
     * Creates the scan summary section
     */
    default ComplianceReportDTO.ScanSummaryDTO createScanSummary(ComplianceReport report) {
        if (report == null || report.getSummary() == null) {
            return null;
        }
        
        ComplianceReport.ScanSummary summary = report.getSummary();
        return ComplianceReportDTO.ScanSummaryDTO.builder()
                .tablesScanned(summary.getTablesScanned())
                .columnsScanned(summary.getColumnsScanned())
                .piiColumnsFound(summary.getPiiColumnsFound())
                .totalPiiCandidates(summary.getTotalPiiCandidates())
                .build();
    }
    
    /**
     * Creates the scan configuration section
     */
    default ComplianceReportDTO.ScanConfigurationDTO createScanConfiguration(ComplianceReport report) {
        if (report == null) {
            return null;
        }
        
        return ComplianceReportDTO.ScanConfigurationDTO.builder()
                .sampling(report.getSamplingConfig())
                .detection(report.getDetectionConfig())
                .build();
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
                    .columns(results.stream().map(this::toDto).toList())
                    .build();
            tableFindings.put(tableName, tableFinding);
        });
        
        return tableFindings;
    }
    
    /**
     * Creates the PII types detected section with categorized PII types
     */
    default ComplianceReportDTO.PiiTypesDetectedDTO createPiiTypesDetected(ComplianceReport report) {
        if (report == null || report.getDetectionResults() == null) {
            return null;
        }
        
        Map<String, Integer> directIdentifiers = new HashMap<>();
        Map<String, Integer> sensitiveData = new HashMap<>();
        Map<String, Integer> dateRelated = new HashMap<>();
        
        // Categorize PII types
        for (DetectionResult result : report.getDetectionResults()) {
            if (result.hasPii()) {
                String piiType = result.getHighestConfidencePiiType();
                if (piiType != null) {
                    // Categorize by PII type
                    if (isDirectIdentifier(piiType)) {
                        directIdentifiers.merge(piiType, 1, Integer::sum);
                    } else if (isDateRelated(piiType)) {
                        dateRelated.merge(piiType, 1, Integer::sum);
                    } else if (isSensitiveData(piiType)) {
                        sensitiveData.merge(piiType, 1, Integer::sum);
                    }
                }
            }
        }
        
        // Count quasi-identifiers
        int quasiIdentifierCount = (int) report.getDetectionResults().stream()
                .filter(DetectionResult::isQuasiIdentifier)
                .count();
        
        return ComplianceReportDTO.PiiTypesDetectedDTO.builder()
                .directIdentifiers(directIdentifiers)
                .sensitiveData(sensitiveData)
                .dateRelated(dateRelated)
                .quasiIdentifiers(quasiIdentifierCount)
                .build();
    }
    
    /**
     * Creates a summary of detection methods used
     */
    default Map<String, Integer> createDetectionMethodsSummary(ComplianceReport report) {
        if (report == null || report.getDetectionResults() == null) {
            return new HashMap<>();
        }
        
        Map<String, Integer> methodCounts = new HashMap<>();
        
        // Count methods used for detection
        for (DetectionResult result : report.getDetectionResults()) {
            List<String> methods = getDetectionMethodsFromCandidates(result);
            if (methods != null && !methods.isEmpty()) {
                for (String method : methods) {
                    methodCounts.merge(method, 1, Integer::sum);
                }
            }
            
            // Add quasi-identifier detection if applicable
            if (result.isQuasiIdentifier()) {
                methodCounts.merge("GRAPH_CORRELATION", 1, Integer::sum);
            }
        }
        
        return methodCounts;
    }
    
    /**
     * Helper method to identify direct identifiers
     */
    default boolean isDirectIdentifier(String piiType) {
        if (piiType == null) return false;
        
        return piiType.equals("EMAIL") || 
               piiType.equals("PERSON_NAME") ||
               piiType.equals("SSN") ||
               piiType.equals("PHONE_NUMBER") ||
               piiType.equals("CREDIT_CARD_NUMBER") ||
               piiType.equals("ADDRESS") ||
               piiType.equals("IP_ADDRESS") ||
               piiType.equals("PASSPORT_NUMBER");
    }
    
    /**
     * Helper method to identify date-related data
     */
    default boolean isDateRelated(String piiType) {
        if (piiType == null) return false;
        
        return piiType.equals("DATE") ||
               piiType.equals("CREDIT_CARD_EXPIRATION_DATE");
    }
    
    /**
     * Helper method to identify sensitive data
     */
    default boolean isSensitiveData(String piiType) {
        if (piiType == null) return false;
        
        return piiType.equals("MEDICAL_CONDITION") ||
               piiType.equals("MEDICATION") ||
               piiType.equals("LOCATION") ||
               piiType.equals("ORGANIZATION");
    }
    
    /**
     * Collects only PII findings (including quasi-identifiers) from the report
     */
    @Named("getPiiFindings")
    default List<DetectionResultDTO> getPiiFindings(ComplianceReport report) {
        if (report == null || report.getDetectionResults() == null) {
            return new ArrayList<>();
        }
        
        // Filter to only include PII and quasi-identifiers
        return report.getDetectionResults().stream()
                .filter(result -> result.hasPii() || result.isQuasiIdentifier())
                .map(this::toDto)
                .toList();
    }
    
    /**
     * Collects all detection results (PII, quasi-identifiers, and non-PII columns)
     */
    @Named("getAllDetectionResults")
    default List<DetectionResultDTO> getAllDetectionResults(ComplianceReport report) {
        if (report == null || report.getDetectionResults() == null) {
            return new ArrayList<>();
        }
        
        // Include all detection results, not just those with PII or quasi-identifiers
        return report.getDetectionResults().stream()
                .map(this::toDto)
                .toList();
    }
    
    /**
     * Convertit Instant en LocalDateTime
     */
    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
    
    /**
     * Compte le nombre total de relations dans un schéma
     */
    default int countRelationships(SchemaInfo schemaInfo) {
        if (schemaInfo == null || schemaInfo.getTables() == null) {
            return 0;
        }
        
        return schemaInfo.getTables().stream()
                .mapToInt(table -> {
                    int count = 0;
                    if (table.getImportedRelationships() != null) {
                        count += table.getImportedRelationships().size();
                    }
                    if (table.getExportedRelationships() != null) {
                        count += table.getExportedRelationships().size();
                    }
                    return count;
                })
                .sum();
    }
    
    /**
     * Helper method to round a double value to two decimal places
     */
    @Named("roundToTwoDecimalPlaces")
    default Double roundToTwoDecimalPlaces(Double value) {
        if (value == null) return null;
        return Math.round(value * 100.0) / 100.0;
    }
}