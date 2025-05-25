package com.privsense.api.dto.config;

import com.privsense.api.dto.base.BaseConfigDTO;
import com.privsense.api.validation.Threshold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Configuration pour la détection de PII.
 * Utilise l'annotation @Threshold pour valider les valeurs entre 0 et 1.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DetectionConfigDTO extends BaseConfigDTO {
    
    @Threshold
    @Builder.Default
    private Double heuristicThreshold = 0.7;
    
    @Threshold
    @Builder.Default
    private Double regexThreshold = 0.8;
    
    @Threshold
    @Builder.Default
    private Double nerThreshold = 0.3;
    
    @Threshold
    @Builder.Default
    private Double reportingThreshold = 0.5;
    
    @Builder.Default
    private Boolean stopPipelineOnHighConfidence = true;
}