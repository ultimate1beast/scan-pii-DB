package com.privsense.api.controller;

import com.privsense.api.dto.ConfigurationDTO;
import com.privsense.api.dto.DetectionRuleDTO;
import com.privsense.api.mapper.ConfigurationMapper;
import com.privsense.core.exception.ConfigurationException;
import com.privsense.core.model.DetectionRule;
import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.service.ConfigurationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing application configuration and detection rules.
 */
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Configuration", description = "APIs for managing application configuration and detection rules")
public class ConfigController {

    private final ConfigurationService configService;
    private final PrivSenseConfigProperties configProperties;
    private final ConfigurationMapper configMapper;

    @GetMapping
    @Operation(
        summary = "Get application configuration",
        description = "Returns the current application configuration"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Configuration retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationDTO.class))
    )
    public ResponseEntity<ConfigurationDTO> getConfiguration() {
        log.debug("REST request to get application configuration");
        return ResponseEntity.ok(configMapper.toConfigurationDto(configProperties));
    }
    
    @PutMapping
    @Operation(
        summary = "Update application configuration",
        description = "Updates the application configuration"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Configuration updated successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ConfigurationDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid configuration")
    public ResponseEntity<ConfigurationDTO> updateConfiguration(
            @RequestBody ConfigurationDTO config) {
        log.debug("REST request to update application configuration");
        try {
            // Convert to map for service
            Map<String, Object> configMap = configService.updateConfiguration(convertToMap(config));
            
            // Return updated configuration
            return ResponseEntity.ok(configMapper.toConfigurationDto(configProperties));
        } catch (ConfigurationException e) {
            log.error("Error updating configuration", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Failed to update configuration: " + e.getMessage());
        }
    }
    
    @GetMapping("/detection-rules")
    @Operation(
        summary = "Get all detection rules",
        description = "Returns all detection rules"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Rules retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DetectionRuleDTO.class))
    )
    public ResponseEntity<List<DetectionRuleDTO>> getDetectionRules() {
        log.debug("REST request to get all detection rules");
        List<DetectionRule> rules = configService.getAllDetectionRules();
        return ResponseEntity.ok(configMapper.toDetectionRuleDtoList(rules));
    }
    
    @PostMapping("/detection-rules")
    @Operation(
        summary = "Create detection rule",
        description = "Creates a new detection rule"
    )
    @ApiResponse(
        responseCode = "201", 
        description = "Rule created successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DetectionRuleDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid rule data")
    public ResponseEntity<DetectionRuleDTO> createDetectionRule(
            @RequestBody DetectionRuleDTO ruleDto) {
        log.debug("REST request to create detection rule: {}", ruleDto.getName());
        try {
            DetectionRule rule = configMapper.toDetectionRule(ruleDto);
            DetectionRule savedRule = configService.createDetectionRule(rule);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(configMapper.toDetectionRuleDto(savedRule));
        } catch (IllegalArgumentException e) {
            log.error("Error creating detection rule", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Failed to create detection rule: " + e.getMessage());
        }
    }
    
    @GetMapping("/detection-rules/{ruleId}")
    @Operation(
        summary = "Get detection rule",
        description = "Returns a specific detection rule"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Rule retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DetectionRuleDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Rule not found")
    public ResponseEntity<DetectionRuleDTO> getDetectionRule(
            @Parameter(description = "ID of the rule", required = true)
            @PathVariable String ruleId) {
        log.debug("REST request to get detection rule: {}", ruleId);
        return configService.getDetectionRule(ruleId)
                .map(configMapper::toDetectionRuleDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Detection rule not found with ID: " + ruleId));
    }
    
    @PutMapping("/detection-rules/{ruleId}")
    @Operation(
        summary = "Update detection rule",
        description = "Updates an existing detection rule"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Rule updated successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DetectionRuleDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid rule data")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    public ResponseEntity<DetectionRuleDTO> updateDetectionRule(
            @Parameter(description = "ID of the rule", required = true)
            @PathVariable String ruleId, 
            @RequestBody DetectionRuleDTO ruleDto) {
        log.debug("REST request to update detection rule: {}", ruleId);
        try {
            DetectionRule rule = configMapper.toDetectionRule(ruleDto);
            DetectionRule updatedRule = configService.updateDetectionRule(ruleId, rule);
            return ResponseEntity.ok(configMapper.toDetectionRuleDto(updatedRule));
        } catch (IllegalArgumentException e) {
            log.error("Error updating detection rule", e);
            if (e.getMessage().contains("not found")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Failed to update detection rule: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/detection-rules/{ruleId}")
    @Operation(
        summary = "Delete detection rule",
        description = "Deletes a detection rule"
    )
    @ApiResponse(responseCode = "204", description = "Rule deleted successfully")
    @ApiResponse(responseCode = "404", description = "Rule not found")
    public ResponseEntity<Void> deleteDetectionRule(
            @Parameter(description = "ID of the rule", required = true)
            @PathVariable String ruleId) {
        log.debug("REST request to delete detection rule: {}", ruleId);
        boolean deleted = configService.deleteDetectionRule(ruleId);
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Detection rule not found with ID: " + ruleId);
        }
    }
    
    /**
     * Converts ConfigurationDTO to a Map for use with the configuration service.
     * 
     * @param dto The ConfigurationDTO
     * @return Map representation of the configuration
     */
    private Map<String, Object> convertToMap(ConfigurationDTO dto) {
        // This is a simplified implementation. In a real application, you would use
        // an object mapper library like Jackson to convert between objects and maps.
        Map<String, Object> config = new java.util.HashMap<>();
        
        if (dto.getDetection() != null) {
            Map<String, Object> detection = new java.util.HashMap<>();
            ConfigurationDTO.DetectionConfigDTO detectionDto = dto.getDetection();
            detection.put("heuristicThreshold", detectionDto.getHeuristicThreshold());
            detection.put("regexThreshold", detectionDto.getRegexThreshold());
            detection.put("nerThreshold", detectionDto.getNerThreshold());
            detection.put("reportingThreshold", detectionDto.getReportingThreshold());
            detection.put("stopPipelineOnHighConfidence", detectionDto.isStopPipelineOnHighConfidence());
            detection.put("entropyEnabled", detectionDto.isEntropyEnabled());
            config.put("detection", detection);
        }
        
        if (dto.getSampling() != null) {
            Map<String, Object> sampling = new java.util.HashMap<>();
            ConfigurationDTO.SamplingConfigDTO samplingDto = dto.getSampling();
            sampling.put("defaultSize", samplingDto.getDefaultSize());
            sampling.put("maxConcurrentDbQueries", samplingDto.getMaxConcurrentDbQueries());
            sampling.put("entropyCalculationEnabled", samplingDto.isEntropyCalculationEnabled());
            sampling.put("defaultMethod", samplingDto.getDefaultMethod());
            config.put("sampling", sampling);
        }
        
        if (dto.getNer() != null) {
            Map<String, Object> ner = new java.util.HashMap<>();
            ConfigurationDTO.NerConfigDTO nerDto = dto.getNer();
            ner.put("enabled", nerDto.isEnabled());
            ner.put("url", nerDto.getServiceUrl());
            ner.put("timeoutSeconds", nerDto.getTimeoutSeconds());
            ner.put("maxSamples", nerDto.getMaxSamples());
            ner.put("retryAttempts", nerDto.getRetryAttempts());
            
            if (nerDto.getCircuitBreaker() != null) {
                Map<String, Object> circuitBreaker = new java.util.HashMap<>();
                circuitBreaker.put("enabled", nerDto.getCircuitBreaker().isEnabled());
                circuitBreaker.put("failureThreshold", nerDto.getCircuitBreaker().getFailureThreshold());
                circuitBreaker.put("resetTimeoutSeconds", nerDto.getCircuitBreaker().getResetTimeoutSeconds());
                ner.put("circuitBreaker", circuitBreaker);
            }
            
            config.put("ner", ner);
        }
        
        if (dto.getReporting() != null) {
            Map<String, Object> reporting = new java.util.HashMap<>();
            ConfigurationDTO.ReportingConfigDTO reportingDto = dto.getReporting();
            reporting.put("pdfEnabled", reportingDto.isPdfEnabled());
            reporting.put("csvEnabled", reportingDto.isCsvEnabled());
            reporting.put("textEnabled", reportingDto.isTextEnabled());
            reporting.put("reportOutputPath", reportingDto.getReportOutputPath());
            config.put("reporting", reporting);
        }
        
        if (dto.getDatabase() != null) {
            Map<String, Object> database = new java.util.HashMap<>();
            ConfigurationDTO.DatabaseConfigDTO databaseDto = dto.getDatabase();
            
            if (databaseDto.getPool() != null) {
                Map<String, Object> pool = new java.util.HashMap<>();
                pool.put("connectionTimeout", databaseDto.getPool().getConnectionTimeout());
                pool.put("idleTimeout", databaseDto.getPool().getIdleTimeout());
                pool.put("maxLifetime", databaseDto.getPool().getMaxLifetime());
                pool.put("minimumIdle", databaseDto.getPool().getMinimumIdle());
                pool.put("maximumPoolSize", databaseDto.getPool().getMaximumPoolSize());
                database.put("pool", pool);
            }
            
            database.put("driverDir", databaseDto.getDriverDir());
            config.put("database", database);
        }
        
        return config;
    }
}