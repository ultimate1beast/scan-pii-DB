package com.privsense.api.dto.base;

import lombok.Data;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Classe de base pour toutes les réponses DTO
 * Standardise la structure des réponses API avec gestion des erreurs
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseResponseDTO {
    
    private String status = "SUCCESS";
    private String errorMessage;
    
    /**
     * Vérifie si la réponse est en succès
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
    
    /**
     * Définit le status en erreur avec un message
     */
    public void setError(String message) {
        this.status = "ERROR";
        this.errorMessage = message;
    }
}