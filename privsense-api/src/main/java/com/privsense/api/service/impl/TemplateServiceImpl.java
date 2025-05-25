package com.privsense.api.service.impl;

import com.privsense.api.dto.config.DetectionConfigDTO;
import com.privsense.api.dto.config.SamplingConfigDTO;
import com.privsense.api.dto.ScanRequest;
import com.privsense.api.dto.ScanTemplateDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.mapper.ScanTemplateMapper;
import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.api.service.TemplateService;
import com.privsense.core.model.ScanTemplate;
import com.privsense.core.repository.ConnectionRepository;
import com.privsense.core.repository.ScanTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the TemplateService for managing scan templates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final ScanTemplateRepository templateRepository;
    private final ConnectionRepository connectionRepository;
    private final ScanOrchestrationService scanOrchestrationService;
    private final ScanTemplateMapper scanTemplateMapper;

    @Override
    @Transactional
    public ScanTemplateDTO createTemplate(ScanTemplateDTO templateDTO) {
        log.debug("Creating new scan template: {}", templateDTO.getName());

        // Validate connection exists
        if (!connectionRepository.existsById(templateDTO.getConnectionId())) {
            throw new IllegalArgumentException("Connection not found: " + templateDTO.getConnectionId());
        }

        // Convert DTO to entity using mapper
        ScanTemplate template = scanTemplateMapper.toEntity(templateDTO);
        
        // Save the entity
        ScanTemplate savedTemplate = templateRepository.save(template);
        
        // Convert back to DTO using mapper and return
        ScanTemplateDTO result = scanTemplateMapper.toDto(savedTemplate);
        result.addMeta("status", "SUCCESS");
        
        log.info("Created scan template with ID: {}", result.getId());
        return result;
    }

    @Override
    @Transactional
    public ScanTemplateDTO updateTemplate(UUID id, ScanTemplateDTO templateDTO) {
        log.debug("Updating scan template with ID: {}", id);
        
        // Find the existing template
        ScanTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        
        // Validate connection exists if it's being changed
        if (!existingTemplate.getConnectionId().equals(templateDTO.getConnectionId()) &&
                !connectionRepository.existsById(templateDTO.getConnectionId())) {
            throw new IllegalArgumentException("Connection not found: " + templateDTO.getConnectionId());
        }
        
        // Update the entity using mapper
        scanTemplateMapper.updateEntityFromDto(templateDTO, existingTemplate);
        
        // Save the updated entity
        ScanTemplate updatedTemplate = templateRepository.save(existingTemplate);
        
        // Convert back to DTO using mapper and return
        ScanTemplateDTO result = scanTemplateMapper.toDto(updatedTemplate);
        result.addMeta("status", "SUCCESS");
        
        log.info("Updated scan template with ID: {}", result.getId());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ScanTemplateDTO getTemplate(UUID id) {
        log.debug("Getting scan template with ID: {}", id);
        
        // Find the template
        ScanTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));
        
        // Convert to DTO using mapper and return
        ScanTemplateDTO result = scanTemplateMapper.toDto(template);
        result.addMeta("status", "SUCCESS");
        
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanTemplateDTO> getAllTemplates() {
        log.debug("Getting all scan templates");
        
        return templateRepository.findAll().stream()
                .map(template -> {
                    ScanTemplateDTO dto = scanTemplateMapper.toDto(template);
                    dto.addMeta("status", "SUCCESS");
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScanTemplateDTO> getTemplatesByConnectionId(UUID connectionId) {
        log.debug("Getting scan templates for connection ID: {}", connectionId);
        
        return templateRepository.findByConnectionId(connectionId).stream()
                .map(template -> {
                    ScanTemplateDTO dto = scanTemplateMapper.toDto(template);
                    dto.addMeta("status", "SUCCESS");
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean deleteTemplate(UUID id) {
        log.debug("Deleting scan template with ID: {}", id);
        
        boolean deleted = templateRepository.deleteById(id);
        
        if (deleted) {
            log.info("Deleted scan template with ID: {}", id);
        } else {
            log.warn("Scan template not found for deletion: {}", id);
        }
        
        return deleted;
    }

    @Override
    @Transactional
    public ScanJobResponse executeScanFromTemplate(UUID templateId, UUID connectionId) {
        log.debug("Executing scan from template ID: {}, with connection override: {}", templateId, connectionId);
        
        // Find the template
        ScanTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + templateId));
        
        // Use the connection ID from the template if not provided
        UUID actualConnectionId = connectionId != null ? connectionId : template.getConnectionId();
        
        // Verify the connection exists
        if (!connectionRepository.existsById(actualConnectionId)) {
            throw new IllegalArgumentException("Connection not found: " + actualConnectionId);
        }
        
        // Create a ScanRequest from the template
        ScanRequest scanRequest = createScanRequestFromTemplate(template, actualConnectionId);
        
        // Submit the scan job
        UUID jobId = scanOrchestrationService.submitScanJob(scanRequest);
        
        // Get the job status to return
        ScanJobResponse response = scanOrchestrationService.getJobStatus(jobId);
        response.addMeta("status", "SUCCESS");
        response.addMeta("fromTemplate", templateId.toString());
        
        log.info("Executed scan from template ID: {}, job ID: {}", templateId, jobId);
        return response;
    }
    
    /**
     * Helper method to create a ScanRequest from a template
     */
    private ScanRequest createScanRequestFromTemplate(ScanTemplate template, UUID connectionId) {
        // Create sampling config
        SamplingConfigDTO samplingConfig = scanTemplateMapper.toSamplingConfigDTO(template);
                
        // Create detection config
        DetectionConfigDTO detectionConfig = scanTemplateMapper.toDetectionConfigDTO(template);
                
        return ScanRequest.builder()
                .connectionId(connectionId)
                .targetTables(template.getTargetTables())
                .samplingConfig(samplingConfig)
                .detectionConfig(detectionConfig)
                .build();
    }
}