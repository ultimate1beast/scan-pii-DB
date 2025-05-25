package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private LocalDateTime endTime;
    private LocalDateTime lastUpdateTime;
    private Integer progress;
    private boolean completed;
    private boolean failed;
    private String errorMessage;
    private String databaseName;
    private String databaseProductName;
    private Integer totalColumnsScanned;
    private Integer totalPiiColumnsFound;
    
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
    
    /**
     * Sets the start time from a string representation.
     * 
     * @param timeStr the time as a string
     */
    public void setStartTime(String timeStr) {
        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                this.startTime = LocalDateTime.parse(timeStr);
            } catch (Exception e) {
                // Try with different format patterns if standard ISO format fails
                try {
                    // Handle timestamps with zone information
                    if (timeStr.contains("T") && timeStr.contains("Z")) {
                        this.startTime = LocalDateTime.parse(timeStr.replace("Z", ""));
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        this.startTime = LocalDateTime.parse(timeStr, formatter);
                    }
                } catch (Exception ex) {
                    // Log error or set to null if parsing fails
                    this.startTime = null;
                }
            }
        } else {
            this.startTime = null;
        }
    }
    
    /**
     * Sets the end time from a string representation.
     * 
     * @param timeStr the time as a string
     */
    public void setEndTime(String timeStr) {
        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                this.endTime = LocalDateTime.parse(timeStr);
            } catch (Exception e) {
                // Try with different format patterns if standard ISO format fails
                try {
                    // Handle timestamps with zone information
                    if (timeStr.contains("T") && timeStr.contains("Z")) {
                        this.endTime = LocalDateTime.parse(timeStr.replace("Z", ""));
                    } else {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        this.endTime = LocalDateTime.parse(timeStr, formatter);
                    }
                } catch (Exception ex) {
                    // Log error or set to null if parsing fails
                    this.endTime = null;
                }
            }
        } else {
            this.endTime = null;
        }
    }
}