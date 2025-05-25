package com.privsense.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.util.Set;

/**
 * Configuration de l'échantillonnage pour une table spécifique.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSamplingRequest {
    @NotNull(message = "Le nom de la table est requis")
    private String tableName;
    
    @NotEmpty(message = "Au moins une colonne doit être spécifiée")
    private Set<String> columnNames;
    
    private Integer sampleSize; // Remplace la configuration par défaut si non null
}