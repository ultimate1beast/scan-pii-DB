package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseRequestDTO;
import com.privsense.api.dto.config.SamplingConfigDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;

import java.util.UUID;

/**
 * DTO pour les requêtes d'échantillonnage d'une table/colonne spécifique.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SamplingRequest extends BaseRequestDTO {
    
    @NotNull(message = "L'ID de connexion est requis")
    private UUID connectionId;
    
    @NotBlank(message = "Le nom de la table est requis")
    private String tableName;
    
    @NotBlank(message = "Le nom de la colonne est requis")
    private String columnName;
    
    @Valid
    @Builder.Default
    private SamplingConfigDTO config = new SamplingConfigDTO();
}