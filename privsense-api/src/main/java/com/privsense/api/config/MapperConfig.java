package com.privsense.api.config;

import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.api.service.ScanOrchestrationService.JobStatus;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionConfig;
import com.privsense.core.model.SamplingConfig;
import org.modelmapper.ModelMapper;
import org.modelmapper.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for object mapping between DTOs and domain models.
 */
@Configuration
public class MapperConfig {

    /**
     * Creates and configures a ModelMapper bean for mapping between DTOs and domain models.
     * 
     * @return A configured ModelMapper instance
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        // Configure DatabaseConnectionRequest -> DatabaseConnectionInfo mapping using TypeMap
        Converter<Boolean, Boolean> sslConverter = ctx -> ctx.getSource() != null ? ctx.getSource() : false;
        modelMapper.createTypeMap(DatabaseConnectionRequest.class, DatabaseConnectionInfo.class)
            .addMappings(mapper -> {
                mapper.using(sslConverter)
                      .map(DatabaseConnectionRequest::getSslEnabled, DatabaseConnectionInfo::setSslEnabled);
                // Other fields are mapped automatically by name
            });

        // Configure DatabaseConnectionInfo -> ConnectionResponse mapping
        modelMapper.typeMap(DatabaseConnectionInfo.class, ConnectionResponse.class)
                .addMappings(mapper -> {
                    // No need to skip setPassword as it likely doesn't exist
                });
        
        // Configure ScanRequest -> SamplingConfig mapping
        modelMapper.createTypeMap(ScanRequest.class, SamplingConfig.class)
                .addMappings(mapper -> {
                    mapper.map(ScanRequest::getSampleSize, SamplingConfig::setSampleSize);
                    mapper.map(ScanRequest::getSamplingMethod, SamplingConfig::setSamplingMethod);
                });
        
        // Configure ScanRequest -> DetectionConfig mapping
        modelMapper.createTypeMap(ScanRequest.class, DetectionConfig.class)
                .addMappings(mapper -> {
                    mapper.map(ScanRequest::getHeuristicThreshold, DetectionConfig::setHeuristicThreshold);
                    mapper.map(ScanRequest::getRegexThreshold, DetectionConfig::setRegexThreshold);
                    mapper.map(ScanRequest::getNerThreshold, DetectionConfig::setNerThreshold);
                });
        
        // Configure JobStatus -> ScanJobResponse mapping
        modelMapper.createTypeMap(JobStatus.class, ScanJobResponse.class)
                .addMappings(mapper -> {
                    mapper.map(JobStatus::getJobId, ScanJobResponse::setJobId);
                    mapper.map(JobStatus::getConnectionId, ScanJobResponse::setConnectionId);
                    // Add null check for getState()
                    mapper.map(src -> src.getState() != null ? src.getState().name() : null, ScanJobResponse::setStatus);
                    mapper.map(JobStatus::getStartTime, ScanJobResponse::setStartTime);
                    mapper.map(JobStatus::getErrorMessage, ScanJobResponse::setErrorMessage);
                    mapper.map(JobStatus::getLastUpdateTime, ScanJobResponse::setLastUpdateTime);

                    // Skip for Progress if it's intentionally not mapped from JobStatus
                    mapper.skip(ScanJobResponse::setProgress); 

                    // Map current operation description using a custom converter
                    mapper.map(source -> {
                        if (source == null || source.getState() == null) {
                            return "Unknown state";
                        }
                        return getCurrentOperation(source.getState());
                    }, ScanJobResponse::setCurrentOperation);
                });
        
        return modelMapper;
    }
    
    /**
     * Helper method to generate a human-readable description of the current operation.
     */
    private String getCurrentOperation(ScanOrchestrationService.JobState state) {
        if (state == null) return "Unknown";
        
        switch (state) {
            case PENDING:
                return "Waiting to start";
            case EXTRACTING_METADATA:
                return "Extracting database metadata";
            case SAMPLING:
                return "Sampling data from columns";
            case DETECTING_PII:
                return "Analyzing data for PII";
            case GENERATING_REPORT:
                return "Generating compliance report";
            case COMPLETED:
                return "Scan completed";
            case FAILED:
                return "Scan failed";
            default:
                return "Unknown";
        }
    }
}