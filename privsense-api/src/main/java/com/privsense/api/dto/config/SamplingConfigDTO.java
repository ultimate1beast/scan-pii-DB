package com.privsense.api.dto.config;

import com.privsense.api.dto.base.BaseConfigDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Configuration pour les opérations d'échantillonnage
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SamplingConfigDTO extends BaseConfigDTO {
    
    @Min(value = 10, message = "La taille d'échantillon doit être d'au moins 10")
    @Max(value = 10000, message = "La taille d'échantillon ne peut pas dépasser 10000")
    @Builder.Default
    private Integer sampleSize = 10;
    
    @Builder.Default
    private String samplingMethod = "RANDOM";
    
    @Builder.Default
    private Boolean entropyCalculationEnabled = false;
    
    private Integer maxConcurrentQueries;
}