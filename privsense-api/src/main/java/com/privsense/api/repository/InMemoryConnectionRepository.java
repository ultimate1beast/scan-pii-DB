package com.privsense.api.repository;

import com.privsense.core.model.DatabaseConnectionInfo;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the ConnectionRepository interface.
 * Stores database connection information in a thread-safe map.
 * 
 * Note: This is a simple implementation for development/testing purposes.
 * In a production environment, connections should be persisted securely.
 */
@Repository
public class InMemoryConnectionRepository implements ConnectionRepository {

    private final Map<UUID, DatabaseConnectionInfo> connections = new ConcurrentHashMap<>();

    @Override
    public UUID save(DatabaseConnectionInfo connectionInfo) {
        // Use the ID from the connection info object if it exists
        UUID connectionId = connectionInfo.getId();
        
        // If no ID is set, generate a new one
        if (connectionId == null) {
            connectionId = UUID.randomUUID();
            connectionInfo.setId(connectionId);
        }
        
        connections.put(connectionId, connectionInfo);
        return connectionId;
    }

    @Override
    public boolean existsById(UUID connectionId) {
        return connections.containsKey(connectionId);
    }

    @Override
    public Optional<DatabaseConnectionInfo> findById(UUID connectionId) {
        return Optional.ofNullable(connections.get(connectionId));
    }

    @Override
    public boolean deleteById(UUID connectionId) {
        return connections.remove(connectionId) != null;
    }
}