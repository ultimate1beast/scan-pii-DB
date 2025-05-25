package com.privsense.api.mapper;

import com.privsense.api.dto.ConfigurationDTO;
import com.privsense.api.dto.DetectionRuleDTO;
import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.model.DetectionRule;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper class for converting between configuration DTOs and domain objects.
 */
@Component
public class ConfigurationMapper {

    /**
     * Converts configuration properties to ConfigurationDTO.
     * 
     * @param properties The configuration properties
     * @return ConfigurationDTO object
     */
    public ConfigurationDTO toConfigurationDto(PrivSenseConfigProperties properties) {
        // Map Detection config
        ConfigurationDTO.DetectionConfigDTO detectionDto = ConfigurationDTO.DetectionConfigDTO.builder()
                .heuristicThreshold(properties.getDetection().getHeuristicThreshold())
                .regexThreshold(properties.getDetection().getRegexThreshold())
                .nerThreshold(properties.getDetection().getNerThreshold())
                .reportingThreshold(properties.getDetection().getReportingThreshold())
                .stopPipelineOnHighConfidence(properties.getDetection().isStopPipelineOnHighConfidence())
                .entropyEnabled(properties.getDetection().isEntropyEnabled())
                .build();
        
        // Map Sampling config
        ConfigurationDTO.SamplingConfigDTO samplingDto = ConfigurationDTO.SamplingConfigDTO.builder()
                .defaultSize(properties.getSampling().getDefaultSize())
                .maxConcurrentDbQueries(properties.getSampling().getMaxConcurrentDbQueries())
                .entropyCalculationEnabled(properties.getSampling().isEntropyCalculationEnabled())
                .defaultMethod(properties.getSampling().getDefaultMethod())
                .build();
        
        // Map NER config
        ConfigurationDTO.NerConfigDTO.CircuitBreakerConfigDTO circuitBreakerDto = 
                ConfigurationDTO.NerConfigDTO.CircuitBreakerConfigDTO.builder()
                .enabled(properties.getNer().getService().getCircuitBreaker().isEnabled())
                .failureThreshold(properties.getNer().getService().getCircuitBreaker().getFailureThreshold())
                .resetTimeoutSeconds(properties.getNer().getService().getCircuitBreaker().getResetTimeoutSeconds())
                .build();
                
        ConfigurationDTO.NerConfigDTO nerDto = ConfigurationDTO.NerConfigDTO.builder()
                .enabled(properties.getNer().isEnabled())
                .serviceUrl(properties.getNer().getService().getUrl())
                .timeoutSeconds(properties.getNer().getService().getTimeoutSeconds())
                .maxSamples(properties.getNer().getService().getMaxSamples())
                .retryAttempts(properties.getNer().getService().getRetryAttempts())
                .circuitBreaker(circuitBreakerDto)
                .build();
        
        // Map Reporting config
        ConfigurationDTO.ReportingConfigDTO reportingDto = ConfigurationDTO.ReportingConfigDTO.builder()
                .pdfEnabled(properties.getReporting().isPdfEnabled())
                .csvEnabled(properties.getReporting().isCsvEnabled())
                .textEnabled(properties.getReporting().isTextEnabled())
                .reportOutputPath(properties.getReporting().getReportOutputPath())
                .build();
        
        // Map Database config
        ConfigurationDTO.DatabaseConfigDTO.PoolConfigDTO poolDto = 
                ConfigurationDTO.DatabaseConfigDTO.PoolConfigDTO.builder()
                .connectionTimeout(properties.getDb().getPool().getConnectionTimeout())
                .idleTimeout(properties.getDb().getPool().getIdleTimeout())
                .maxLifetime(properties.getDb().getPool().getMaxLifetime())
                .minimumIdle(properties.getDb().getPool().getMinimumIdle())
                .maximumPoolSize(properties.getDb().getPool().getMaximumPoolSize())
                .build();
                
        ConfigurationDTO.DatabaseConfigDTO databaseDto = ConfigurationDTO.DatabaseConfigDTO.builder()
                .pool(poolDto)
                .driverDir(properties.getDb().getJdbc().getDriverDir())
                .build();
        
        // Build complete DTO
        return ConfigurationDTO.builder()
                .detection(detectionDto)
                .sampling(samplingDto)
                .ner(nerDto)
                .reporting(reportingDto)
                .database(databaseDto)
                .build();
    }
    
    /**
     * Maps DetectionRule entity to DetectionRuleDTO.
     * 
     * @param rule DetectionRule entity
     * @return DetectionRuleDTO
     */
    public DetectionRuleDTO toDetectionRuleDto(DetectionRule rule) {
        return DetectionRuleDTO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .pattern(rule.getPattern())
                .piiType(rule.getPiiType())
                .confidenceScore(rule.getConfidenceScore())
                .description(rule.getDescription())
                .enabled(rule.isEnabled())
                .ruleType(mapRuleType(rule.getRuleType()))
                .build();
    }
    
    /**
     * Maps DetectionRuleDTO to DetectionRule entity.
     * 
     * @param dto DetectionRuleDTO
     * @return DetectionRule entity
     */
    public DetectionRule toDetectionRule(DetectionRuleDTO dto) {
        return DetectionRule.builder()
                .id(dto.getId())
                .name(dto.getName())
                .pattern(dto.getPattern())
                .piiType(dto.getPiiType())
                .confidenceScore(dto.getConfidenceScore())
                .description(dto.getDescription())
                .enabled(dto.isEnabled())
                .ruleType(mapRuleType(dto.getRuleType()))
                .build();
    }
    
    /**
     * Maps a list of DetectionRule entities to DetectionRuleDTO list.
     * 
     * @param rules List of DetectionRule entities
     * @return List of DetectionRuleDTO
     */
    public List<DetectionRuleDTO> toDetectionRuleDtoList(List<DetectionRule> rules) {
        return rules.stream()
                .map(this::toDetectionRuleDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Maps DetectionRule.RuleType to DetectionRuleDTO.RuleType.
     * 
     * @param ruleType Entity rule type
     * @return DTO rule type
     */
    private DetectionRuleDTO.RuleType mapRuleType(DetectionRule.RuleType ruleType) {
        if (ruleType == null) {
            return null;
        }
        
        switch (ruleType) {
            case REGEX:
                return DetectionRuleDTO.RuleType.REGEX;
            case HEURISTIC:
                return DetectionRuleDTO.RuleType.HEURISTIC;
            case NER:
                return DetectionRuleDTO.RuleType.NER;
            default:
                throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        }
    }
    
    /**
     * Maps DetectionRuleDTO.RuleType to DetectionRule.RuleType.
     * 
     * @param ruleType DTO rule type
     * @return Entity rule type
     */
    private DetectionRule.RuleType mapRuleType(DetectionRuleDTO.RuleType ruleType) {
        if (ruleType == null) {
            return null;
        }
        
        switch (ruleType) {
            case REGEX:
                return DetectionRule.RuleType.REGEX;
            case HEURISTIC:
                return DetectionRule.RuleType.HEURISTIC;
            case NER:
                return DetectionRule.RuleType.NER;
            default:
                throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        }
    }
}