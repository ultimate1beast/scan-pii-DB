package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.BatchSamplingRequest;
import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.dto.SamplingRequest;
import com.privsense.api.dto.SamplingResponse;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.config.DetectionConfigDTO;
import com.privsense.api.dto.config.SamplingConfigDTO;
import com.privsense.api.dto.result.ColumnSamplingResult;
import com.privsense.api.dto.result.TableSamplingResult;

import com.privsense.api.service.impl.ScanJobManagementServiceImpl.JobStatus;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionConfig;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SamplingConfig;
import com.privsense.core.model.ScanMetadata;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Interface pour le mapping entre DTOs et modèles de domaine utilisant MapStruct.
 * Utilise la configuration centralisée MapStructConfig.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class,
    imports = {com.privsense.api.dto.DatabaseConnectionRequest.class}
)
public interface DtoMapper {

    DtoMapper INSTANCE = Mappers.getMapper(DtoMapper.class);
    
    /**
     * Map DatabaseConnectionRequest DTO vers DatabaseConnectionInfo modèle de domaine.
     */
    @Mapping(source = "driverClassName", target = "jdbcDriverClass")
    @Mapping(source = "sslEnabled", target = "sslEnabled", defaultValue = "false")
    @Mapping(target = "sslTrustStorePath", ignore = true)
    @Mapping(target = "sslTrustStorePassword", ignore = true)
    @Mapping(target = "id", ignore = true) // Ignore ID comme il est généré plus tard
    DatabaseConnectionInfo toDomainModel(DatabaseConnectionRequest request);
    
    /**
     * Map DatabaseConnectionInfo modèle de domaine vers ConnectionResponse DTO.
     */
    @Mapping(target = "connectionId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "databaseProductName", ignore = true)
    @Mapping(target = "databaseProductVersion", ignore = true)
    ConnectionResponse toDto(DatabaseConnectionInfo connectionInfo);
    
    /**
     * Map SamplingConfigDTO vers SamplingConfig modèle de domaine.
     */
    @Mapping(source = "sampleSize", target = "sampleSize")
    @Mapping(source = "samplingMethod", target = "samplingMethod")
    @Mapping(source = "entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "maxConcurrentQueries", target = "maxConcurrentQueries")
    SamplingConfig toSamplingConfig(SamplingConfigDTO configDto);

    /**
     * Map DetectionConfigDTO vers DetectionConfig modèle de domaine.
     */
    @Mapping(source = "heuristicThreshold", target = "heuristicThreshold")
    @Mapping(source = "regexThreshold", target = "regexThreshold")
    @Mapping(source = "nerThreshold", target = "nerThreshold")
    @Mapping(source = "reportingThreshold", target = "reportingThreshold")
    @Mapping(source = "stopPipelineOnHighConfidence", target = "stopPipelineOnHighConfidence")
    DetectionConfig toDetectionConfig(DetectionConfigDTO configDto);
    
    /**
     * Map ScanRequest DTO vers SamplingConfig modèle de domaine.
     */
    @Mapping(source = "samplingConfig", target = ".")
    SamplingConfig toSamplingConfig(ScanRequest request);
    
    /**
     * Map ScanRequest DTO vers DetectionConfig modèle de domaine.
     */
    @Mapping(source = "detectionConfig", target = ".")
    DetectionConfig toDetectionConfig(ScanRequest request);
    
    /**
     * Map directement ScanJobResponse (idempotent).
     */
    @Mapping(target = "currentOperation", qualifiedByName = "stateToOperation")
    ScanJobResponse toDto(ScanJobResponse jobStatus);
    
    /**
     * Map ScanMetadata vers ScanJobResponse.
     */
    @Mapping(source = "id", target = "jobId")
    @Mapping(source = "status", target = "status", qualifiedByName = "scanStatusToString")
    @Mapping(source = "status", target = "currentOperation", qualifiedByName = "scanStatusToOperation")
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "endTime", target = "lastUpdateTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "errorMessage", target = "errorMessage")
    ScanJobResponse fromScanMetadata(ScanMetadata scanMetadata);
    
    /**
     * Map JobStatus vers ScanJobResponse.
     */
    @Mapping(source = "jobId", target = "jobId")
    @Mapping(source = "connectionId", target = "connectionId")
    @Mapping(expression = "java(status.getState().name())", target = "status")
    @Mapping(expression = "java(stateToOperation(status.getState().name()))", target = "currentOperation")
    @Mapping(source = "startTime", target = "startTime")
    @Mapping(source = "lastUpdateTime", target = "lastUpdateTime")
    @Mapping(source = "errorMessage", target = "errorMessage")
    ScanJobResponse fromJobStatus(JobStatus status);
    
    /**
     * Convertit Instant en LocalDateTime.
     */
    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
    
    /**
     * Convertit ScanMetadata.ScanStatus en chaîne.
     */
    @Named("scanStatusToString")
    default String scanStatusToString(ScanMetadata.ScanStatus status) {
        return status != null ? status.name() : null;
    }
    
    /**
     * Convertit ScanMetadata.ScanStatus en description d'opération.
     */
    @Named("scanStatusToOperation")
    default String scanStatusToOperation(ScanMetadata.ScanStatus status) {
        return status != null ? stateToOperation(status.name()) : null;
    }
    
    /**
     * Convertit ScanMetadata.ScanStatus en description d'opération lisible.
     */
    @Named("stateToOperation")
    default String stateToOperation(String state) {
        if (state == null) return "Inconnu";
        
        switch (state) {
            case "PENDING": return "En attente";
            case "EXTRACTING_METADATA": return "Extraction des métadonnées";
            case "SAMPLING": return "Échantillonnage des colonnes";
            case "DETECTING_PII": return "Analyse des données pour PII";
            case "GENERATING_REPORT": return "Génération du rapport de conformité";
            case "COMPLETED": return "Scan terminé";
            case "FAILED": return "Scan échoué";
            default: return "Inconnu";
        }
    }
    
    /**
     * Convertit une liste d'objets en liste de chaînes.
     */
    @Named("objectListToStringList")
    default List<String> objectListToStringList(List<Object> objectList) {
        if (objectList == null) {
            return null;
        }
        return objectList.stream()
            .map(obj -> obj != null ? obj.toString() : null)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Map SamplingRequest DTO vers SamplingConfig modèle de domaine.
     */
    SamplingConfig toSamplingConfig(SamplingRequest request);
    
    /**
     * Map SampleData et SamplingConfig vers SamplingResponse DTO.
     */
    @Mapping(target = "tableName", ignore = true)
    @Mapping(target = "columnName", ignore = true)
    @Mapping(source = "sampleData.totalRowCount", target = "actualRowCount")
    @Mapping(source = "sampleData.totalNullCount", target = "nullCount")
    @Mapping(source = "sampleData.nullPercentage", target = "nullPercentage")
    @Mapping(source = "sampleData.nonNullPercentage", target = "nonNullPercentage")
    @Mapping(source = "sampleData.entropy", target = "entropy")
    @Mapping(target = "entropyCalculated", expression = "java(sampleData.getEntropy() != null)")
    @Mapping(source = "sampleData.samples", target = "sampleValues", qualifiedByName = "objectListToStringList")
    @Mapping(target = "samplingMethod", source = "config.samplingMethod")
    @Mapping(target = "sampleSize", source = "config.sampleSize")
    @Mapping(target = "samplingTimeMs", ignore = true)
    @Mapping(target = "valueDistribution", ignore = true)
    @Mapping(target = "status", constant = "SUCCESS")
    SamplingResponse toSamplingResponse(SampleData sampleData, SamplingConfig config);
    
    /**
     * Map BatchSamplingRequest DTO vers SamplingConfig modèle de domaine.
     */
    @Mapping(source = "defaultConfig", target = ".")
    SamplingConfig toSamplingConfig(BatchSamplingRequest request);
    
    /**
     * Map SampleData vers ColumnSamplingResult pour la réponse par lots.
     */
    @Mapping(target = "columnName", source = "columnInfo.columnName")
    @Mapping(source = "sampleData.totalRowCount", target = "actualRowCount")
    @Mapping(source = "sampleData.totalNullCount", target = "nullCount")
    @Mapping(source = "sampleData.nullPercentage", target = "nullPercentage")
    @Mapping(source = "sampleData.nonNullPercentage", target = "nonNullPercentage")
    @Mapping(source = "sampleData.entropy", target = "entropy")
    @Mapping(target = "entropyCalculated", expression = "java(sampleData.getEntropy() != null)")
    @Mapping(target = "samplingMethod", source = "config.samplingMethod")
    @Mapping(target = "sampleSize", source = "config.sampleSize")
    @Mapping(target = "topValues", ignore = true)
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    ColumnSamplingResult toColumnSamplingResult(
            SampleData sampleData, ColumnInfo columnInfo, SamplingConfig config);
            
    /**
     * Crée un TableSamplingRequest à partir d'un BatchSamplingRequest.TableSamplingRequest
     */
    @Mapping(source = "tableName", target = "tableName")
    TableSamplingResult toTableSamplingRequest(
            BatchSamplingRequest.TableSamplingRequest source);
}