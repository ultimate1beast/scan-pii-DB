package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO pour les requêtes de connexion à la base de données.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatabaseConnectionRequest extends BaseRequestDTO {
    
    @NotBlank(message = "L'hôte est requis")
    private String host;
    
    @NotNull(message = "Le port est requis")
    private Integer port;
    
    @NotBlank(message = "Le nom de la base de données est requis")
    private String databaseName;
    
    @NotBlank(message = "Le nom d'utilisateur est requis")
    private String username;
    
    
    private String password;
    
    @NotBlank(message = "La classe de pilote JDBC est requise")
    private String driverClassName;
    
    private Boolean sslEnabled;
    
    private String sslTrustStorePath;
    
    private String sslTrustStorePassword;
}