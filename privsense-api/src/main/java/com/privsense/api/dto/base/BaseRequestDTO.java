package com.privsense.api.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Classe de base pour toutes les requêtes DTO.
 * Standardise la structure des requêtes API et permet l'ajout
 * de propriétés communes à l'avenir.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseRequestDTO implements Serializable {
    /**
     * Identifiant de version pour la sérialisation.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Identifiant unique de la requête pour le traçage et la corrélation.
     * Si non spécifié, un ID sera généré côté serveur.
     */
    @Schema(hidden = true)
    private UUID requestId;
    
    /**
     * Horodatage de la requête côté client.
     */
    @Schema(hidden = true)
    private Instant requestTimestamp;
    
    /**
     * Informations sur le client qui fait la requête (optionnel).
     */
    @Schema(hidden = true)
    private String clientInfo;
    
    /**
     * Code de version de l'API utilisée par le client (optionnel).
     * Utile pour la gestion des versions et la compatibilité.
     */
    @Schema(hidden = true)
    private String apiVersion;
}