package com.privsense.api.mapper;

import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.api.service.ScanOrchestrationService.JobStatus;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionConfig;
import com.privsense.core.model.SamplingConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

/**
 * Interface for mapping between DTOs and domain models using MapStruct.
 */
@Mapper(componentModel = "spring")
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
     * Maps JobStatus to ScanJobResponse DTO.
     */
    @Mapping(source = "state", target = "currentOperation", qualifiedByName = "stateToOperation")
    @Mapping(source = "jobId", target = "jobId")
    @Mapping(source = "connectionId", target = "connectionId")
    @Mapping(expression = "java(jobStatus.getState().toString())", target = "status")
    @Mapping(source = "startTime", target = "startTime")
    @Mapping(source = "lastUpdateTime", target = "lastUpdateTime")
    @Mapping(source = "errorMessage", target = "errorMessage")
    @Mapping(target = "progress", ignore = true)
    ScanJobResponse toDto(JobStatus jobStatus);
    
    /**
     * Converts JobState to a human-readable operation description.
     */
    @Named("stateToOperation")
    default String stateToOperation(ScanOrchestrationService.JobState state) {
        if (state == null) return "Unknown";
        
        switch (state) {
            case PENDING: return "Waiting to start";
            case EXTRACTING_METADATA: return "Extracting database metadata";
            case SAMPLING: return "Sampling data from columns";
            case DETECTING_PII: return "Analyzing data for PII";
            case GENERATING_REPORT: return "Generating compliance report";
            case COMPLETED: return "Scan completed";
            case FAILED: return "Scan failed";
            default: return "Unknown";
        }
    }
}