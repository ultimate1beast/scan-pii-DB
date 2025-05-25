package com.privsense.api.service;

import com.privsense.api.dto.ScanTemplateDTO;
import com.privsense.api.dto.ScanJobResponse;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing scan templates.
 */
public interface TemplateService {

    /**
     * Creates a new scan template.
     *
     * @param templateDTO The template data
     * @return The created template with ID
     */
    ScanTemplateDTO createTemplate(ScanTemplateDTO templateDTO);

    /**
     * Updates an existing scan template.
     *
     * @param id The template ID
     * @param templateDTO The updated template data
     * @return The updated template
     * @throws IllegalArgumentException if the template doesn't exist
     */
    ScanTemplateDTO updateTemplate(UUID id, ScanTemplateDTO templateDTO);

    /**
     * Gets a scan template by its ID.
     *
     * @param id The template ID
     * @return The template
     * @throws IllegalArgumentException if the template doesn't exist
     */
    ScanTemplateDTO getTemplate(UUID id);

    /**
     * Gets all scan templates.
     *
     * @return A list of all templates
     */
    List<ScanTemplateDTO> getAllTemplates();

    /**
     * Gets templates for a specific connection.
     *
     * @param connectionId The connection ID
     * @return A list of templates for the connection
     */
    List<ScanTemplateDTO> getTemplatesByConnectionId(UUID connectionId);

    /**
     * Deletes a scan template.
     *
     * @param id The template ID
     * @return true if the template was deleted, false if it didn't exist
     */
    boolean deleteTemplate(UUID id);

    /**
     * Executes a scan using a template.
     *
     * @param templateId The template ID
     * @param connectionId Optional connection ID to override the one in the template
     * @return The scan job response with job ID and status
     * @throws IllegalArgumentException if the template doesn't exist
     */
    ScanJobResponse executeScanFromTemplate(UUID templateId, UUID connectionId);
}