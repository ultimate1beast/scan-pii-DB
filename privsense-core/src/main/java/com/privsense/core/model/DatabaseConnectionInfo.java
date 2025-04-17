package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID; // Import UUID

/**
 * Holds all necessary details to establish a database connection.
 * Password handling should be enhanced with proper security measures
 * (e.g., Spring's property encryption or external secrets management).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseConnectionInfo {
    
    private UUID id; // Add ID field
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String password;
    private String jdbcDriverClass;
    
    // SSL parameters
    private Boolean sslEnabled;
    private String sslTrustStorePath;
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