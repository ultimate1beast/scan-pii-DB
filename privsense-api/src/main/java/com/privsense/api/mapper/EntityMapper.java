package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.SchemaInfoDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SchemaInfo;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Mapper pour la conversion entre entités de domaine et DTOs.
 * Utilise MapStruct à la place de ModelMapper pour de meilleures performances.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class
)
public interface EntityMapper {
    
    /**
     * Convertit SchemaInfo en SchemaInfoDTO
     */
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "tables", target = "tables")
    @Mapping(target = "totalTableCount", expression = "java(schemaInfo.getTables() != null ? schemaInfo.getTables().size() : 0)")
    @Mapping(target = "totalColumnCount", expression = "java(schemaInfo.getTotalColumnCount())")
    @Mapping(target = "totalRelationshipCount", expression = "java(countRelationships(schemaInfo))")
    SchemaInfoDTO toSchemaDto(SchemaInfo schemaInfo);
    
    /**
     * Convertit DatabaseConnectionInfo en ConnectionResponse
     */
    @Mapping(source = "id", target = "connectionId")
    @Mapping(target = "databaseProductName", ignore = true)
    @Mapping(target = "databaseProductVersion", ignore = true)
    @Mapping(target = "status", constant = "UNKNOWN")
    @Mapping(target = "errorMessage", ignore = true)
    com.privsense.api.dto.ConnectionResponse toConnectionResponse(DatabaseConnectionInfo connectionInfo);

    /**
     * Convertit ComplianceReport vers ComplianceReportDTO
     */
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "piiFindings", target = "piiFindings")
    @Mapping(source = "quasiIdentifierFindings", target = "quasiIdentifierFindings")
    @Mapping(source = "correlatedColumnGroups", target = "correlatedColumnGroups")
    @Mapping(source = "summary", target = "summary") 
    ComplianceReportDTO toDto(ComplianceReport report);
    
    /**
     * Convertit DetectionResult vers DetectionResultDTO
     */
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    @Mapping(source = "highestConfidencePiiType", target = "piiType")
    @Mapping(source = "highestConfidenceScore", target = "confidenceScore")
    @Mapping(source = "detectionMethods", target = "detectionMethods")
    @Mapping(source = "columnInfo.columnName", target = "columnName")
    @Mapping(source = "columnInfo.table.tableName", target = "tableName")
    @Mapping(source = "columnInfo.databaseTypeName", target = "dataType")
    @Mapping(source = "quasiIdentifier", target = "isQuasiIdentifier")
    @Mapping(source = "quasiIdentifierRiskScore", target = "quasiIdentifierRiskScore")
    @Mapping(source = "quasiIdentifierType", target = "quasiIdentifierType")
    @Mapping(source = "correlatedColumns", target = "correlatedColumns")
    @Mapping(target = "sensitiveData", expression = "java(detectionResult.hasPii())")
    DetectionResultDTO toDto(DetectionResult detectionResult);
    
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
}