package com.privsense.api.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import jakarta.validation.Valid;
import java.io.Serializable;

/**
 * Classe de base pour tous les DTOs de configuration.
 * Cette classe permet de standardiser les propriétés communes à toutes les configurations
 * et facilite l'évolution de l'architecture en permettant l'ajout de nouvelles propriétés
 * de manière centralisée.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseConfigDTO implements Serializable {
    
    /**
     * Identifiant de version pour la sérialisation.
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * Champ pour indiquer si cette configuration est par défaut.
     * Utile pour différencier les configurations personnalisées des configurations par défaut.
     */
    private Boolean isDefault;
    
    /**
     * Nom descriptif optionnel pour cette configuration.
     */
    private String configName;
}