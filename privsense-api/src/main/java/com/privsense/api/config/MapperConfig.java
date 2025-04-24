package com.privsense.api.config;

import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionConfig;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SamplingConfig;
import com.privsense.core.model.ScanMetadata;
import org.modelmapper.ModelMapper;
import org.modelmapper.Converter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.stream.Collectors;

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
            .addMappings(mapper -> mapper.using(sslConverter)
                      .map(DatabaseConnectionRequest::getSslEnabled, DatabaseConnectionInfo::setSslEnabled));

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
        
        // Configure ScanMetadata -> ScanJobResponse mapping
        modelMapper.createTypeMap(ScanMetadata.class, ScanJobResponse.class)
                .addMappings(mapper -> {
                    mapper.map(ScanMetadata::getId, ScanJobResponse::setJobId);
                    mapper.map(ScanMetadata::getConnectionId, ScanJobResponse::setConnectionId);
                    // Map status directly
                    mapper.map(src -> src.getStatus() != null ? src.getStatus().name() : null, ScanJobResponse::setStatus);
                    mapper.map(src -> src.getStartTime() != null ? src.getStartTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null, ScanJobResponse::setStartTime);
                    mapper.map(ScanMetadata::getErrorMessage, ScanJobResponse::setErrorMessage);
                    mapper.map(src -> src.getEndTime() != null ? src.getEndTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null, ScanJobResponse::setLastUpdateTime);

                    // Skip for Progress if it's intentionally not mapped
                    mapper.skip(ScanJobResponse::setProgress); 

                    // Map current operation description using the status string
                    mapper.map(source -> {
                        if (source == null || source.getStatus() == null) {
                            return "Unknown state";
                        }
                        return getCurrentOperation(source.getStatus().name());
                    }, ScanJobResponse::setCurrentOperation);
                });
                
        // Configure ComplianceReport -> ComplianceReportDTO mapping
        modelMapper.createTypeMap(ComplianceReport.class, ComplianceReportDTO.class)
                .addMappings(mapper -> {
                    // Map basic properties
                    mapper.map(ComplianceReport::getScanId, ComplianceReportDTO::setScanId);
                    mapper.map(ComplianceReport::getReportId, ComplianceReportDTO::setReportId);
                    mapper.map(ComplianceReport::getDatabaseHost, ComplianceReportDTO::setDatabaseHost);
                    mapper.map(ComplianceReport::getDatabaseName, ComplianceReportDTO::setDatabaseName);
                    mapper.map(ComplianceReport::getDatabaseProductName, ComplianceReportDTO::setDatabaseProductName);
                    mapper.map(ComplianceReport::getDatabaseProductVersion, ComplianceReportDTO::setDatabaseProductVersion);
                    mapper.map(ComplianceReport::getTotalTablesScanned, ComplianceReportDTO::setTotalTablesScanned);
                    mapper.map(ComplianceReport::getTotalColumnsScanned, ComplianceReportDTO::setTotalColumnsScanned);
                    mapper.map(ComplianceReport::getTotalPiiColumnsFound, ComplianceReportDTO::setTotalPiiColumnsFound);
                    mapper.map(ComplianceReport::getScanStartTime, ComplianceReportDTO::setScanStartTime);
                    mapper.map(ComplianceReport::getScanEndTime, ComplianceReportDTO::setScanEndTime);
                    mapper.map(ComplianceReport::getScanDuration, ComplianceReportDTO::setScanDuration);
                    mapper.map(ComplianceReport::getSamplingConfig, ComplianceReportDTO::setSamplingConfig);
                    mapper.map(ComplianceReport::getDetectionConfig, ComplianceReportDTO::setDetectionConfig);
                    
                    // Map PII findings to DTO objects with null safety
                    mapper.map(src -> {
                        if (src == null || src.getPiiFindings() == null) {
                            return Collections.emptyList();
                        }
                        return src.getPiiFindings().stream()
                                .map(finding -> mapDetectionResultToPiiColumnDTO(finding))
                                .collect(Collectors.toList());
                    }, ComplianceReportDTO::setPiiFindings);
                });
        
        return modelMapper;
    }
    
    /**
     * Helper method to map a DetectionResult to PiiColumnDTO
     */
    private ComplianceReportDTO.PiiColumnDTO mapDetectionResultToPiiColumnDTO(DetectionResult result) {
        return ComplianceReportDTO.PiiColumnDTO.builder()
                .tableName(result.getColumnInfo().getTable().getTableName())
                .columnName(result.getColumnInfo().getColumnName())
                .dataType(result.getColumnInfo().getDatabaseTypeName())
                .piiType(result.getHighestConfidencePiiType())
                .confidenceScore(result.getHighestConfidenceScore())
                .detectionMethods(result.getDetectionMethods())
                .build();
    }
    
    /**
     * Helper method to generate a human-readable description of the current operation.
     */
    private String getCurrentOperation(String state) {
        if (state == null) return "Unknown";
        
        switch (state) {
            case "PENDING":
                return "Waiting to start";
            case "EXTRACTING_METADATA":
                return "Extracting database metadata";
            case "SAMPLING":
                return "Sampling data from columns";
            case "DETECTING_PII":
                return "Analyzing data for PII";
            case "GENERATING_REPORT":
                return "Generating compliance report";
            case "COMPLETED":
                return "Scan completed";
            case "FAILED":
                return "Scan failed";
            default:
                return "Unknown";
        }
    }
}