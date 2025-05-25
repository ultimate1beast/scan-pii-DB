package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Data Transfer Object pour les réponses de connexion à la base de données.
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConnectionResponse extends BaseResponseDTO {

    private UUID connectionId;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String databaseProductName;
    private String databaseProductVersion;
    private Boolean sslEnabled;
    private String status;
    
    /**
     * Override to consider "CONNECTED" and "SUCCESS" as successful states
     * 
     * @return true if the status indicates success, false otherwise
     */
    @Override
    public boolean isSuccess() {
        // Check if status is one of the success states
        return "SUCCESS".equals(status) || "CONNECTED".equals(status) || "AVAILABLE".equals(status);
    }
}