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

import java.util.List;
import java.util.Set;
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
    private SamplingConfigDTO defaultConfig = new SamplingConfigDTO();
    
    @NotEmpty(message = "Au moins une table doit être spécifiée")
    private List<TableSamplingRequest> tables;
    
    /**
     * Configuration de l'échantillonnage pour une table spécifique.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TableSamplingRequest {
        @NotNull(message = "Le nom de la table est requis")
        private String tableName;
        
        @NotEmpty(message = "Au moins une colonne doit être spécifiée")
        private Set<String> columnNames;
        
        private Integer sampleSize; // Remplace la configuration par défaut si non null
    }
}