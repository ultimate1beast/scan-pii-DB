package com.privsense.api.mapper;

import com.privsense.api.dto.ScanTemplateDTO;
import com.privsense.api.dto.config.DetectionConfigDTO;
import com.privsense.api.dto.config.SamplingConfigDTO;
import com.privsense.core.model.ScanTemplate;
import org.mapstruct.*;

/**
 * Mapper for converting between ScanTemplate entities and DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScanTemplateMapper {

    /**
     * Converts a ScanTemplate entity to its corresponding DTO.
     *
     * @param entity The ScanTemplate entity
     * @return The ScanTemplateDTO
     */
    @Mapping(source = "samplingSize", target = "samplingConfig.sampleSize")
    @Mapping(source = "samplingMethod", target = "samplingConfig.samplingMethod")
    @Mapping(source = "entropyCalculationEnabled", target = "samplingConfig.entropyCalculationEnabled")
    @Mapping(source = "heuristicThreshold", target = "detectionConfig.heuristicThreshold")
    @Mapping(source = "regexThreshold", target = "detectionConfig.regexThreshold")
    @Mapping(source = "nerThreshold", target = "detectionConfig.nerThreshold")
    @Mapping(source = "reportingThreshold", target = "detectionConfig.reportingThreshold")
    @Mapping(source = "stopPipelineOnHighConfidence", target = "detectionConfig.stopPipelineOnHighConfidence")
    ScanTemplateDTO toDto(ScanTemplate entity);

    /**
     * Converts a ScanTemplateDTO to its corresponding entity.
     *
     * @param dto The ScanTemplateDTO
     * @return The ScanTemplate entity
     */
    @Mapping(source = "samplingConfig.sampleSize", target = "samplingSize")
    @Mapping(source = "samplingConfig.samplingMethod", target = "samplingMethod")
    @Mapping(source = "samplingConfig.entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "detectionConfig.heuristicThreshold", target = "heuristicThreshold")
    @Mapping(source = "detectionConfig.regexThreshold", target = "regexThreshold")
    @Mapping(source = "detectionConfig.nerThreshold", target = "nerThreshold")
    @Mapping(source = "detectionConfig.reportingThreshold", target = "reportingThreshold")
    @Mapping(source = "detectionConfig.stopPipelineOnHighConfidence", target = "stopPipelineOnHighConfidence")
    ScanTemplate toEntity(ScanTemplateDTO dto);

    /**
     * Updates an existing ScanTemplate entity with data from a DTO.
     *
     * @param dto The source DTO with updated data
     * @param entity The entity to update
     * @return The updated entity
     */
    @Mapping(source = "samplingConfig.sampleSize", target = "samplingSize")
    @Mapping(source = "samplingConfig.samplingMethod", target = "samplingMethod")
    @Mapping(source = "samplingConfig.entropyCalculationEnabled", target = "entropyCalculationEnabled")
    @Mapping(source = "detectionConfig.heuristicThreshold", target = "heuristicThreshold")
    @Mapping(source = "detectionConfig.regexThreshold", target = "regexThreshold")
    @Mapping(source = "detectionConfig.nerThreshold", target = "nerThreshold")
    @Mapping(source = "detectionConfig.reportingThreshold", target = "reportingThreshold")
    @Mapping(source = "detectionConfig.stopPipelineOnHighConfidence", target = "stopPipelineOnHighConfidence")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ScanTemplate updateEntityFromDto(ScanTemplateDTO dto, @MappingTarget ScanTemplate entity);

    /**
     * Creates a SamplingConfigDTO from entity fields.
     */
    @Named("toSamplingConfigDTO")
    default SamplingConfigDTO toSamplingConfigDTO(ScanTemplate entity) {
        if (entity == null) {
            return null;
        }
        return SamplingConfigDTO.builder()
                .sampleSize(entity.getSamplingSize())
                .samplingMethod(entity.getSamplingMethod())
                .entropyCalculationEnabled(entity.getEntropyCalculationEnabled())
                .build();
    }

    /**
     * Creates a DetectionConfigDTO from entity fields.
     */
    @Named("toDetectionConfigDTO")
    default DetectionConfigDTO toDetectionConfigDTO(ScanTemplate entity) {
        if (entity == null) {
            return null;
        }
        return DetectionConfigDTO.builder()
                .heuristicThreshold(entity.getHeuristicThreshold())
                .regexThreshold(entity.getRegexThreshold())
                .nerThreshold(entity.getNerThreshold())
                .reportingThreshold(entity.getReportingThreshold())
                .stopPipelineOnHighConfidence(entity.getStopPipelineOnHighConfidence())
                .build();
    }
}