package com.privsense.api.controller;

import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanTemplateDTO;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.service.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing scan templates.
 */
@RestController
@RequestMapping("/api/v1/scan-templates")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Scan Templates", description = "APIs for managing reusable scan configurations")
public class ScanTemplateController {

    private final TemplateService templateService;

    /**
     * Creates a new scan template.
     */
    @PostMapping
    @Operation(
        summary = "Create scan template",
        description = "Saves a new scan template for future use"
    )
    @ApiResponse(
        responseCode = "201", 
        description = "Template created successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanTemplateDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid template data")
    @ApiResponse(responseCode = "404", description = "Connection not found")
    public ResponseEntity<ScanTemplateDTO> createTemplate(
            @Valid @RequestBody 
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Template data", 
                required = true, 
                content = @Content(schema = @Schema(implementation = ScanTemplateDTO.class))
            ) ScanTemplateDTO template) {
        
        log.debug("REST request to create scan template: {}", template.getName());
        
        try {
            ScanTemplateDTO createdTemplate = templateService.createTemplate(template);
            return ResponseEntity
                    .created(URI.create("/api/v1/scan-templates/" + createdTemplate.getId()))
                    .body(createdTemplate);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    /**
     * Updates an existing scan template.
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Update scan template",
        description = "Updates an existing scan template"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Template updated successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanTemplateDTO.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid template data")
    @ApiResponse(responseCode = "404", description = "Template or connection not found")
    public ResponseEntity<ScanTemplateDTO> updateTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ScanTemplateDTO template) {
        
        log.debug("REST request to update scan template: {}", id);
        
        try {
            ScanTemplateDTO updatedTemplate = templateService.updateTemplate(id, template);
            return ResponseEntity.ok(updatedTemplate);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    /**
     * Gets all scan templates.
     */
    @GetMapping
    @Operation(
        summary = "Get all templates",
        description = "Returns all available scan templates"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "List of templates retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanTemplateDTO.class))
    )
    public ResponseEntity<List<ScanTemplateDTO>> getAllTemplates(
            @Parameter(description = "Filter by database connection ID")
            @RequestParam(required = false) UUID connectionId) {
        
        log.debug("REST request to get all templates, connectionId filter: {}", connectionId);
        
        List<ScanTemplateDTO> templates;
        if (connectionId != null) {
            templates = templateService.getTemplatesByConnectionId(connectionId);
        } else {
            templates = templateService.getAllTemplates();
        }
        
        return ResponseEntity.ok(templates);
    }

    /**
     * Gets a specific scan template.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get template",
        description = "Returns the specified scan template"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Template retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanTemplateDTO.class))
    )
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<ScanTemplateDTO> getTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID id) {
        
        log.debug("REST request to get template: {}", id);
        
        try {
            ScanTemplateDTO template = templateService.getTemplate(id);
            return ResponseEntity.ok(template);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    /**
     * Deletes a scan template.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete template",
        description = "Deletes the specified scan template"
    )
    @ApiResponse(responseCode = "204", description = "Template deleted successfully")
    @ApiResponse(responseCode = "404", description = "Template not found")
    public ResponseEntity<Void> deleteTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID id) {
        
        log.debug("REST request to delete template: {}", id);
        
        boolean deleted = templateService.deleteTemplate(id);
        
        if (deleted) {
            return ResponseEntity.noContent().build();
        } else {
            throw new ResourceNotFoundException("Template not found: " + id);
        }
    }

    /**
     * Executes a scan job using a template.
     */
    @PostMapping("/{id}/execute")
    @Operation(
        summary = "Execute scan from template",
        description = "Starts a new scan using the specified template configuration"
    )
    @ApiResponse(
        responseCode = "201", 
        description = "Scan job created successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ScanJobResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "Template or connection not found")
    public ResponseEntity<ScanJobResponse> executeScanFromTemplate(
            @Parameter(description = "Template ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Override the connection ID from the template")
            @RequestParam(required = false) UUID connectionId) {
        
        log.debug("REST request to execute scan from template: {}, connectionId override: {}", id, connectionId);
        
        try {
            ScanJobResponse jobResponse = templateService.executeScanFromTemplate(id, connectionId);
            return ResponseEntity
                    .created(URI.create("/api/v1/scans/" + jobResponse.getJobId()))
                    .body(jobResponse);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }
}