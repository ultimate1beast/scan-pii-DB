package com.privsense.api.repository;

import com.privsense.core.model.DatabaseConnectionInfo;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing database connections.
 */
public interface ConnectionRepository {

    /**
     * Saves a database connection and returns its unique identifier.
     *
     * @param connectionInfo the connection information to save
     * @return the UUID assigned to the connection
     */
    UUID save(DatabaseConnectionInfo connectionInfo);
    
    /**
     * Checks if a connection with the given ID exists.
     *
     * @param connectionId the connection ID to check
     * @return true if the connection exists, false otherwise
     */
    boolean existsById(UUID connectionId);
    
    /**
     * Finds a connection by its ID.
     *
     * @param connectionId the connection ID to find
     * @return an Optional containing the connection if found, or empty if not found
     */
    Optional<DatabaseConnectionInfo> findById(UUID connectionId);
    
    /**
     * Deletes a connection by its ID.
     *
     * @param connectionId the connection ID to delete
     * @return true if the connection was deleted, false if it didn't exist
     */
    boolean deleteById(UUID connectionId);
}