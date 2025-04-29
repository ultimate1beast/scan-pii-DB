package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object pour les réponses des jobs de scan.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ScanJobResponse extends BaseResponseDTO {
    
    private UUID jobId;
    private UUID connectionId;
    private String status;
    private String currentOperation;
    private LocalDateTime startTime;
    private LocalDateTime lastUpdateTime;
    private Integer progress;
    
    /**
     * Vérifie si le job de scan est terminé.
     * 
     * @return true si le statut du job est "COMPLETED", false sinon
     */
    public boolean isCompleted() {
        if (status == null) {
            return false;
        }
        // Vérifie à la fois la correspondance exacte "COMPLETED" et la vérification insensible à la casse
        return "COMPLETED".equals(status) || status.toUpperCase().contains("COMPLETED");
    }
}