package com.privsense.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

/**
 * Holds all necessary details to establish a database connection.
 * Password handling should be enhanced with proper security measures
 * (e.g., Spring's property encryption or external secrets management).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "privsense_database_connections")
public class DatabaseConnectionInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(name = "database_name", nullable = false)
    private String databaseName;
    
    @Column(nullable = false)
    private String username;
    
    @Column
    private String password;
    
    @Column(name = "jdbc_driver_class", nullable = false)
    private String jdbcDriverClass;
    
    // SSL parameters
    @Column(name = "ssl_enabled")
    private Boolean sslEnabled;
    
    @Column(name = "ssl_trust_store_path")
    private String sslTrustStorePath;
    
    @Column(name = "ssl_trust_store_password")
    private String sslTrustStorePassword;
    
    /**
     * Builds a JDBC URL based on the stored connection information.
     * This is a simplified implementation; actual production code would 
     * need logic to handle different database types.
     * 
     * @return A JDBC URL string
     */
    public String buildJdbcUrl() {
        if (jdbcDriverClass == null || host == null || databaseName == null) {
            throw new IllegalStateException("Incomplete connection information. Driver class, host, and database name are required.");
        }
        
        String baseUrl;
        if (jdbcDriverClass.contains("mysql")) {
            baseUrl = "jdbc:mysql://" + host;
        } else if (jdbcDriverClass.contains("postgresql")) {
            baseUrl = "jdbc:postgresql://" + host;
        } else if (jdbcDriverClass.contains("sqlserver")) {
            baseUrl = "jdbc:sqlserver://" + host;
        } else if (jdbcDriverClass.contains("oracle")) {
            baseUrl = "jdbc:oracle:thin:@" + host;
        } else {
            throw new IllegalStateException("Unsupported JDBC driver: " + jdbcDriverClass);
        }
        
        if (port != null) {
            baseUrl += ":" + port;
        }
        
        baseUrl += "/" + databaseName;
        
        if (sslEnabled != null && sslEnabled) {
            baseUrl += "?useSSL=true";
        }
        
        return baseUrl;
    }
    
    @Override
    public String toString() {
        // Don't include password in string representation for security
        return "DatabaseConnectionInfo{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", databaseName='" + databaseName + '\'' +
                ", username='" + username + '\'' +
                ", jdbcDriverClass='" + jdbcDriverClass + '\'' +
                ", sslEnabled=" + sslEnabled +
                '}';
    }
}