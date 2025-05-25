package com.privsense.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.privsense.api.dto.config.SamplingConfigDTO;
import com.privsense.api.dto.config.DetectionConfigDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for scan templates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanTemplateDTO {

    private UUID id;

    @NotBlank(message = "Template name is required")
    @Size(max = 255, message = "Template name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @NotNull(message = "Connection ID is required")
    private UUID connectionId;

    private List<String> targetTables;

    private SamplingConfigDTO samplingConfig;
    
    private DetectionConfigDTO detectionConfig;
    
    @Builder.Default
    private Map<String, Object> meta = new HashMap<>();
    
    /**
     * Helper method to add metadata
     */
    public void addMeta(String key, Object value) {
        if (this.meta == null) {
            this.meta = new HashMap<>();
        }
        this.meta.put(key, value);
    }
}