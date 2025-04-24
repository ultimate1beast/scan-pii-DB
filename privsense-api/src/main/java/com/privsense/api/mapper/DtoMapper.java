package com.privsense.api.mapper;

import com.privsense.api.dto.BatchSamplingRequest;
import com.privsense.api.dto.BatchSamplingResponse;
import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.dto.SamplingRequest;
import com.privsense.api.dto.SamplingResponse;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;

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
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Interface for mapping between DTOs and domain models using MapStruct.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    imports = {com.privsense.api.dto.DatabaseConnectionRequest.class}
)
public interface DtoMapper {

    DtoMapper INSTANCE = Mappers.getMapper(DtoMapper.class);
    
    /**
     * Maps DatabaseConnectionRequest DTO to DatabaseConnectionInfo domain model.
     */
    @Mapping(source = "driverClassName", target = "jdbcDriverClass")
    @Mapping(source = "sslEnabled", target = "sslEnabled", defaultValue = "false")
    @Mapping(target = "sslTrustStorePath", ignore = true)
    @Mapping(target = "sslTrustStorePassword", ignore = true)
    @Mapping(target = "id", ignore = true) // Ignore ID as it's generated later
    DatabaseConnectionInfo toDomainModel(DatabaseConnectionRequest request);
    
    /**
     * Maps DatabaseConnectionInfo domain model to ConnectionResponse DTO.
     * Properties that need to be set manually are marked as ignored.
     */
    @Mapping(target = "connectionId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "databaseProductName", ignore = true)
    @Mapping(target = "databaseProductVersion", ignore = true)
    ConnectionResponse toDto(DatabaseConnectionInfo connectionInfo);
    
    /**
     * Maps ScanRequest DTO to SamplingConfig domain model.
     */
    @Mapping(target = "maxConcurrentQueries", ignore = true)
    SamplingConfig toSamplingConfig(ScanRequest request);
    
    /**
     * Maps ScanRequest DTO to DetectionConfig domain model.
     */
    @Mapping(target = "reportingThreshold", ignore = true)
    @Mapping(target = "stopPipelineOnHighConfidence", ignore = true)
    DetectionConfig toDetectionConfig(ScanRequest request);
    
    /**
     * Maps ScanJobResponse to ScanJobResponse DTO (direct transfer).
     */
    @Mapping(target = "currentOperation", qualifiedByName = "stateToOperation")
    ScanJobResponse toDto(ScanJobResponse jobStatus);
    
    /**
     * Maps ScanMetadata to ScanJobResponse.
     */
    @Mapping(source = "id", target = "jobId")
    @Mapping(source = "status", target = "status", qualifiedByName = "scanStatusToString")
    @Mapping(source = "status", target = "currentOperation", qualifiedByName = "scanStatusToOperation")
    @Mapping(source = "startTime", target = "startTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "endTime", target = "lastUpdateTime", qualifiedByName = "instantToLocalDateTime")
    @Mapping(source = "errorMessage", target = "errorMessage")
    ScanJobResponse fromScanMetadata(ScanMetadata scanMetadata);
    
    /**
     * Maps JobStatus to ScanJobResponse.
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
     * Converts Instant to LocalDateTime.
     */
    @Named("instantToLocalDateTime")
    default LocalDateTime instantToLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
    
    /**
     * Converts ScanMetadata.ScanStatus to string.
     */
    @Named("scanStatusToString")
    default String scanStatusToString(ScanMetadata.ScanStatus status) {
        return status != null ? status.name() : null;
    }
    
    /**
     * Converts ScanMetadata.ScanStatus to operation description.
     */
    @Named("scanStatusToOperation")
    default String scanStatusToOperation(ScanMetadata.ScanStatus status) {
        return status != null ? stateToOperation(status.name()) : null;
    }
    
    /**
     * Converts ScanMetadata.ScanStatus to a human-readable operation description.
     */
    @Named("stateToOperation")
    default String stateToOperation(String state) {
        if (state == null) return "Unknown";
        
        switch (state) {
            case "PENDING": return "Waiting to start";
            case "EXTRACTING_METADATA": return "Extracting database metadata";
            case "SAMPLING": return "Sampling data from columns";
            case "DETECTING_PII": return "Analyzing data for PII";
            case "GENERATING_REPORT": return "Generating compliance report";
            case "COMPLETED": return "Scan completed";
            case "FAILED": return "Scan failed";
            default: return "Unknown";
        }
    }
    
    /**
     * Maps SamplingRequest DTO to SamplingConfig domain model.
     */
    @Mapping(source = "sampleSize", target = "sampleSize")
    @Mapping(source = "samplingMethod", target = "samplingMethod")
    @Mapping(source = "calculateEntropy", target = "entropyCalculationEnabled")
    @Mapping(target = "maxConcurrentQueries", ignore = true)
    SamplingConfig toSamplingConfig(SamplingRequest request);
    
    /**
     * Maps SampleData and SamplingConfig to SamplingResponse DTO.
     * Note: This mapping is partial and requires manual post-processing
     * for complex fields like valueDistribution.
     */
    @Mapping(target = "tableName", ignore = true)  // Set manually
    @Mapping(target = "columnName", ignore = true) // Set manually
    @Mapping(source = "sampleData.totalRowCount", target = "actualRowCount")
    @Mapping(source = "sampleData.totalNullCount", target = "nullCount")
    @Mapping(source = "sampleData.nullPercentage", target = "nullPercentage")
    @Mapping(source = "sampleData.nonNullPercentage", target = "nonNullPercentage")
    @Mapping(source = "sampleData.entropy", target = "entropy")
    @Mapping(target = "entropyCalculated", expression = "java(sampleData.getEntropy() != null)")
    @Mapping(source = "sampleData.samples", target = "sampleValues")
    @Mapping(target = "samplingMethod", source = "config.samplingMethod")
    @Mapping(target = "sampleSize", source = "config.sampleSize")
    @Mapping(target = "samplingTimeMs", ignore = true) // Set manually
    @Mapping(target = "valueDistribution", ignore = true) // Set manually
    SamplingResponse toSamplingResponse(SampleData sampleData, SamplingConfig config);
    
    /**
     * Maps BatchSamplingRequest to SamplingConfig domain model.
     * This is used for creating a default configuration for the batch operation.
     */
    @Mapping(source = "defaultSampleSize", target = "sampleSize")
    @Mapping(source = "defaultSamplingMethod", target = "samplingMethod")
    @Mapping(source = "calculateEntropy", target = "entropyCalculationEnabled")
    @Mapping(target = "maxConcurrentQueries", ignore = true)
    SamplingConfig toSamplingConfig(BatchSamplingRequest request);
    
    /**
     * Maps SampleData to a ColumnSamplingResult for the batch response.
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
    @Mapping(target = "topValues", ignore = true) // Set manually
    @Mapping(target = "status", constant = "SUCCESS")
    @Mapping(target = "errorMessage", ignore = true)
    BatchSamplingResponse.ColumnSamplingResult toColumnSamplingResult(
            SampleData sampleData, ColumnInfo columnInfo, SamplingConfig config);
            
    /**
     * Creates a TableSamplingRequest object from a BatchSamplingRequest.TableSamplingRequest
     */
    @Mapping(source = "tableName", target = "tableName")
    @Mapping(source = "columnNames", target = "columnNames")
    @Mapping(source = "sampleSize", target = "sampleSize")
    BatchSamplingRequest.TableSamplingRequest toTableSamplingRequest(
            BatchSamplingRequest.TableSamplingRequest source);
}