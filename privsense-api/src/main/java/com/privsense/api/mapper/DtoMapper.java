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
import com.privsense.api.dto.TableSamplingRequest;

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
 * Interface for mapping between DTOs and domain models using MapStruct.
 * Uses the centralized MapStructConfig configuration.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class,
    imports = {com.privsense.api.dto.DatabaseConnectionRequest.class},
    builder = @org.mapstruct.Builder(disableBuilder = false)
)
public interface DtoMapper {

    DtoMapper INSTANCE = Mappers.getMapper(DtoMapper.class);
    
    // Define a constant for UTC time zone to ensure consistent time handling across the application
    ZoneId UTC_ZONE = ZoneId.of("UTC");
    
    /**
     * Maps DatabaseConnectionRequest DTO to DatabaseConnectionInfo domain model.
     * 
     * @param request The connection request DTO
     * @return The domain model with connection details
     */
    @Mapping(source = "driverClassName", target = "jdbcDriverClass")
    @Mapping(source = "sslEnabled", target = "sslEnabled", defaultValue = "false")
    @Mapping(target = "sslTrustStorePath", ignore = true) // Not required for basic connection
    @Mapping(target = "sslTrustStorePassword", ignore = true) // Not required for basic connection
    @Mapping(target = "id", ignore = true) // ID is generated by the persistence layer
    DatabaseConnectionInfo toDomainModel(DatabaseConnectionRequest request);
    
    /**
     * Maps DatabaseConnectionInfo domain model to ConnectionResponse DTO.
     * Some fields are ignored here as they're populated separately with dynamic values.
     * 
     * @param connectionInfo The domain model with connection details
     * @return The connection response DTO
     */
    @Mapping(target = "connectionId", ignore = true) // Set dynamically after persistence
    @Mapping(target = "status", ignore = true) // Set dynamically after connection test
    @Mapping(target = "databaseProductName", ignore = true) // Set dynamically from metadata
    @Mapping(target = "databaseProductVersion", ignore = true) // Set dynamically from metadata
    ConnectionResponse toDto(DatabaseConnectionInfo connectionInfo);
    
    /**
     * Maps ConnectionResponse DTO back to DatabaseConnectionInfo domain model.
     * This completes the bidirectional mapping capability.
     * 
     * @param response The connection response DTO
     * @return The domain model with connection details
     */
    @Mapping(target = "jdbcDriverClass", constant = "org.postgresql.Driver") // Default driver for PostgreSQL
    @Mapping(target = "host", source = "host")
    @Mapping(target = "port", source = "port")
    @Mapping(target = "databaseName", source = "databaseName")
    @Mapping(target = "username", source = "username") 
    @Mapping(target = "password", ignore = true) // Passwords should not be included in responses
    @Mapping(target = "sslEnabled", source = "sslEnabled")
    @Mapping(target = "sslTrustStorePath", ignore = true) // Not included in response
    @Mapping(target = "sslTrustStorePassword", ignore = true) // Not included in response
    @Mapping(source = "connectionId", target = "id")
    DatabaseConnectionInfo toDomainModel(ConnectionResponse response);
    
    /**
     * Utility method to build JDBC URL from connection response components.
     * Uses a more flexible approach that supports multiple database types.
     * 
     * @param response The connection response DTO
     * @return The JDBC URL for the database connection, or null if required data is missing
     */
    default String buildJdbcUrl(ConnectionResponse response) {
        if (response == null || response.getHost() == null || response.getDatabaseName() == null) {
            return null;
        }
        
        String baseUrl;
        String productName = response.getDatabaseProductName();
        
        // Determine database type based on product name
        if (productName == null) {
            // Default to PostgreSQL if product name is not available
            baseUrl = "jdbc:postgresql://" + response.getHost();
        } else if (productName.toLowerCase().contains("mysql")) {
            baseUrl = "jdbc:mysql://" + response.getHost();
        } else if (productName.toLowerCase().contains("postgresql")) {
            baseUrl = "jdbc:postgresql://" + response.getHost();
        } else if (productName.toLowerCase().contains("sqlserver") || productName.toLowerCase().contains("microsoft sql server")) {
            baseUrl = "jdbc:sqlserver://" + response.getHost();
        } else if (productName.toLowerCase().contains("oracle")) {
            baseUrl = "jdbc:oracle:thin:@" + response.getHost();
        } else {
            // Unknown database type, use PostgreSQL as default
            baseUrl = "jdbc:postgresql://" + response.getHost();
        }
        
        // Add port if available
        if (response.getPort() != null) {
            baseUrl += ":" + response.getPort();
        }
        
        // Add database name
        baseUrl += "/" + response.getDatabaseName();
        
        // Add SSL if enabled
        if (response.getSslEnabled() != null && response.getSslEnabled()) {
            baseUrl += "?useSSL=true";
        }
        
        return baseUrl;
    }
    
    /**
     * Maps SamplingConfigDTO to SamplingConfig domain model.
     * 
     * @param configDto The sampling configuration DTO
     * @return The domain model with sampling configuration
     */
    @Mapping(source = "sampleSize", target = "sampleSize")
    @Mapping(source = "samplingMethod", target = "samplingMethod")
    @Mapping(source = "entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "maxConcurrentQueries", target = "maxConcurrentQueries")
    SamplingConfig toSamplingConfig(SamplingConfigDTO configDto);

    /**
     * Maps SamplingConfig domain model back to SamplingConfigDTO.
     * Completes the bidirectional mapping capability.
     * 
     * @param config The sampling configuration domain model
     * @return The sampling configuration DTO
     */
    @Mapping(source = "sampleSize", target = "sampleSize")
    @Mapping(source = "samplingMethod", target = "samplingMethod")
    @Mapping(source = "entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "maxConcurrentQueries", target = "maxConcurrentQueries")
    SamplingConfigDTO toDto(SamplingConfig config);
    
    /**
     * Maps DetectionConfigDTO to DetectionConfig domain model.
     * 
     * @param configDto The detection configuration DTO
     * @return The domain model with detection configuration
     */
    @Mapping(source = "heuristicThreshold", target = "heuristicThreshold")
    @Mapping(source = "regexThreshold", target = "regexThreshold")
    @Mapping(source = "nerThreshold", target = "nerThreshold")
    @Mapping(source = "reportingThreshold", target = "reportingThreshold")
    @Mapping(source = "stopPipelineOnHighConfidence", target = "stopPipelineOnHighConfidence")
    DetectionConfig toDetectionConfig(DetectionConfigDTO configDto);
    
    /**
     * Maps DetectionConfig domain model back to DetectionConfigDTO.
     * Completes the bidirectional mapping capability.
     * 
     * @param config The detection configuration domain model
     * @return The detection configuration DTO
     */
    @Mapping(source = "heuristicThreshold", target = "heuristicThreshold")
    @Mapping(source = "regexThreshold", target = "regexThreshold")
    @Mapping(source = "nerThreshold", target = "nerThreshold")
    @Mapping(source = "reportingThreshold", target = "reportingThreshold")
    @Mapping(source = "stopPipelineOnHighConfidence", target = "stopPipelineOnHighConfidence")
    DetectionConfigDTO toDto(DetectionConfig config);
    
    /**
     * Extracts SamplingConfig from ScanRequest DTO.
     * 
     * @param request The scan request containing sampling configuration
     * @return The sampling configuration domain model
     */
    @Mapping(source = "samplingConfig", target = ".")
    SamplingConfig toSamplingConfig(ScanRequest request);
    
    /**
     * Extracts DetectionConfig from ScanRequest DTO.
     * 
     * @param request The scan request containing detection configuration
     * @return The detection configuration domain model
     */
    @Mapping(source = "detectionConfig", target = ".")
    DetectionConfig toDetectionConfig(ScanRequest request);
    
    /**
     * Maps ScanJobResponse directly (idempotent operation).
     * Used to ensure consistent operation description.
     * 
     * @param jobStatus The scan job response to process
     * @return The processed scan job response with consistent operation description
     */
    @Mapping(target = "currentOperation", qualifiedByName = "stateToOperation")
    ScanJobResponse toDto(ScanJobResponse jobStatus);
    
    /**
     * Maps ScanMetadata domain model to ScanJobResponse DTO.
     * 
     * @param scanMetadata The scan metadata domain model
     * @return The scan job response DTO
     */
    @Mapping(source = "id", target = "jobId")
    @Mapping(source = "status", target = "status", qualifiedByName = "scanStatusToString")
    @Mapping(source = "status", target = "currentOperation", qualifiedByName = "scanStatusToOperation")
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "endTime", target = "endTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "endTime", target = "lastUpdateTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "errorMessage", target = "errorMessage")
    ScanJobResponse fromScanMetadata(ScanMetadata scanMetadata);
    
    /**
     * Maps JobStatus to ScanJobResponse DTO.
     * 
     * @param status The job status from the service implementation
     * @return The scan job response DTO
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
     * Converts Instant to LocalDateTime using UTC time zone.
     * This ensures consistent time handling across the application.
     * 
     * @param instant The instant to convert
     * @return The LocalDateTime in UTC time zone
     */
    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, UTC_ZONE) : null;
    }
    
    /**
     * Converts ScanMetadata.ScanStatus to string representation.
     * 
     * @param status The scan status enum
     * @return The string representation of the status
     */
    @Named("scanStatusToString")
    default String scanStatusToString(ScanMetadata.ScanStatus status) {
        return status != null ? status.name() : null;
    }
    
    /**
     * Converts ScanMetadata.ScanStatus to operation description.
     * 
     * @param status The scan status enum
     * @return The human-readable operation description
     */
    @Named("scanStatusToOperation")
    default String scanStatusToOperation(ScanMetadata.ScanStatus status) {
        return status != null ? stateToOperation(status.name()) : null;
    }
    
    /**
     * Converts ScanMetadata.ScanStatus string to human-readable operation description.
     * 
     * @param state The string representation of the scan status
     * @return The human-readable operation description
     */
    @Named("stateToOperation")
    default String stateToOperation(String state) {
        if (state == null) return "Unknown";
        
        switch (state) {
            case "PENDING": return "Pending";
            case "EXTRACTING_METADATA": return "Extracting metadata";
            case "SAMPLING": return "Sampling columns";
            case "DETECTING_PII": return "Analyzing data for PII";
            case "GENERATING_REPORT": return "Generating compliance report";
            case "COMPLETED": return "Scan completed";
            case "FAILED": return "Scan failed";
            default: return "Unknown";
        }
    }
    
    /**
     * Converts a list of objects to a list of strings.
     * 
     * @param objectList The list of objects to convert
     * @return The list of string representations
     */
    @Named("objectListToStringList")
    default List<String> objectListToStringList(List<Object> objectList) {
        if (objectList == null) {
            return java.util.Collections.emptyList();
        }
        return objectList.stream()
            .map(obj -> obj != null ? obj.toString() : null)
            .toList();
    }
    
    /**
     * Maps SamplingRequest DTO to SamplingConfig domain model.
     * 
     * @param request The sampling request DTO
     * @return The sampling configuration domain model
     */
    @Mapping(source = "config.sampleSize", target = "sampleSize")
    @Mapping(source = "config.samplingMethod", target = "samplingMethod")
    @Mapping(source = "config.entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "config.maxConcurrentQueries", target = "maxConcurrentQueries")
    SamplingConfig toSamplingConfig(SamplingRequest request);
    
    /**
     * Maps SampleData and SamplingConfig to SamplingResponse DTO.
     * 
     * @param sampleData The sample data domain model
     * @param config The sampling configuration
     * @return The sampling response DTO
     */
    @Mapping(target = "tableName", ignore = true) // Set by the caller
    @Mapping(target = "columnName", ignore = true) // Set by the caller
    @Mapping(source = "sampleData.totalRowCount", target = "actualRowCount")
    @Mapping(source = "sampleData.totalNullCount", target = "nullCount")
    @Mapping(source = "sampleData.nullPercentage", target = "nullPercentage")
    @Mapping(source = "sampleData.nonNullPercentage", target = "nonNullPercentage")
    @Mapping(source = "sampleData.entropy", target = "entropy")
    @Mapping(target = "entropyCalculated", expression = "java(sampleData.getEntropy() != null)")
    @Mapping(source = "sampleData.samples", target = "sampleValues", qualifiedByName = "objectListToStringList")
    @Mapping(target = "samplingMethod", source = "config.samplingMethod")
    @Mapping(target = "sampleSize", source = "config.sampleSize")
    @Mapping(target = "samplingTimeMs", ignore = true) // Set by the caller
    @Mapping(target = "valueDistribution", ignore = true) // Set by the caller
    SamplingResponse toSamplingResponse(SampleData sampleData, SamplingConfig config);
    
    /**
     * Adds metadata after mapping SamplingResponse
     */
    @org.mapstruct.AfterMapping
    default void addSamplingResponseMetadata(@org.mapstruct.MappingTarget SamplingResponse response) {
        response.addMeta("status", "SUCCESS");
    }
    
    /**
     * Maps BatchSamplingRequest DTO to SamplingConfig domain model.
     * 
     * @param request The batch sampling request DTO
     * @return The sampling configuration domain model
     */
    @Mapping(source = "defaultConfig.sampleSize", target = "sampleSize")
    @Mapping(source = "defaultConfig.samplingMethod", target = "samplingMethod")
    @Mapping(source = "defaultConfig.entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "defaultConfig.maxConcurrentQueries", target = "maxConcurrentQueries")
    SamplingConfig toSamplingConfig(BatchSamplingRequest request);
    
    /**
     * Maps SampleData to ColumnSamplingResult for batch response.
     * 
     * @param sampleData The sample data domain model
     * @param columnInfo The column information
     * @param config The sampling configuration
     * @return The column sampling result DTO
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
    @Mapping(target = "topValues", ignore = true) // Populated separately after mapping
    ColumnSamplingResult toColumnSamplingResult(
            SampleData sampleData, ColumnInfo columnInfo, SamplingConfig config);
            
    /**
     * Adds metadata after mapping ColumnSamplingResult
     */
    @org.mapstruct.AfterMapping
    default void addColumnSamplingResultMetadata(@org.mapstruct.MappingTarget ColumnSamplingResult result) {
        result.addMeta("status", "SUCCESS");
    }
    
    /**
     * Creates a TableSamplingResult from a TableSamplingRequest.
     * 
     * @param source The table sampling request DTO
     * @return The table sampling result DTO
     */
    @Mapping(source = "tableName", target = "tableName")
    TableSamplingResult toTableSamplingRequest(TableSamplingRequest source);
}