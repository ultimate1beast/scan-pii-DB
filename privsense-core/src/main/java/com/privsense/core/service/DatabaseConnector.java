package com.privsense.core.service;

import com.privsense.core.model.DatabaseConnectionInfo;

import java.sql.Connection;
import java.util.UUID;

/**
 * Defines methods for managing database connections.
 */
public interface DatabaseConnector {
    
    /**
     * Establishes a connection to the database using the provided connection information.
     * Returns a unique connection ID that can be used to retrieve the connection later.
     * 
     * @param connectionInfo The database connection details
     * @return A unique identifier for the established connection
     * @throws com.privsense.core.exception.DatabaseConnectionException if connection fails
     */
    UUID connect(DatabaseConnectionInfo connectionInfo);
    
    /**
     * Disconnects and releases resources for a specific connection.
     * 
     * @param connectionId The unique connection identifier
     * @return true if successfully disconnected, false if the connection was not found
     */
    boolean disconnect(UUID connectionId);
    
    /**
     * Retrieves a connection object for the specified connection ID.
     * 
     * @param connectionId The unique connection identifier
     * @return A JDBC Connection object
     * @throws com.privsense.core.exception.DatabaseConnectionException if the connection cannot be retrieved
     */
    Connection getConnection(UUID connectionId);
    
    /**
     * Checks if a connection is still valid/active.
     * 
     * @param connectionId The unique connection identifier
     * @return true if the connection is valid, false otherwise
     */
    boolean isConnectionValid(UUID connectionId);
    
    /**
     * Returns information about the connection.
     * Note: This should NOT include sensitive information like passwords.
     * 
     * @param connectionId The unique connection identifier
     * @return The connection information with sensitive data masked
     */
    DatabaseConnectionInfo getConnectionInfo(UUID connectionId);
}