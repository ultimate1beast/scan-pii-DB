package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseRequestDTO;
import com.privsense.api.dto.config.DetectionConfigDTO;
import com.privsense.api.dto.config.SamplingConfigDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object pour les requêtes de scan.
 * Utilise la composition plutôt que la duplication des propriétés.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ScanRequest extends BaseRequestDTO {
    
    @NotNull(message = "L'ID de connexion est requis")
    private UUID connectionId;
    
    private List<String> targetTables;
    
    @Valid
    @NotNull
    @Builder.Default
    private SamplingConfigDTO samplingConfig = new SamplingConfigDTO();
    
    @Valid
    @NotNull
    @Builder.Default
    private DetectionConfigDTO detectionConfig = new DetectionConfigDTO();
}