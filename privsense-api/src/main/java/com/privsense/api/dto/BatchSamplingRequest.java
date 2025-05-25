package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseRequestDTO;
import com.privsense.api.dto.config.SamplingConfigDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
 * DTO pour les requêtes d'échantillonnage par lots sur plusieurs tables.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BatchSamplingRequest extends BaseRequestDTO {
    
    @NotNull(message = "L'ID de connexion est requis")
    private UUID connectionId;
    
    @Valid
    @Builder.Default
    private SamplingConfigDTO defaultConfig = new SamplingConfigDTO();
    
    @NotEmpty(message = "Au moins une table doit être spécifiée")
    private List<TableSamplingRequest> tables;
}