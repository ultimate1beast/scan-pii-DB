package com.privsense.api.controller;

import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.mapper.DtoMapper;
import com.privsense.api.repository.jpa.ConnectionJpaRepository;
import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.service.DatabaseConnector;
import com.privsense.core.service.MetadataExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing database connections.
 */
@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
@Tag(name = "Database Connections", description = "APIs for managing database connections")
public class ConnectionController {

    private final DatabaseConnector databaseConnector;
    private final MetadataExtractor metadataExtractor;
    private final ConnectionJpaRepository connectionRepository;
    private final DtoMapper dtoMapper;

    /**
     * Creates a new database connection.
     */
    @PostMapping
    @Operation(
        summary = "Create a new database connection",
        description = "Establishes a connection to the database using the provided credentials"
    )
    @ApiResponse(responseCode = "200", description = "Connection successful")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "503", description = "Unable to connect to database")
    public ResponseEntity<ConnectionResponse> createConnection(
            @Valid @RequestBody DatabaseConnectionRequest request) {

        // Convert DTO to domain model using DtoMapper (proper mapstruct mapper)
        DatabaseConnectionInfo connectionInfo = dtoMapper.toDomainModel(request);

        try {
            // Try connecting first to get the actual connection ID used by the connector
            UUID connectionId = databaseConnector.connect(connectionInfo); // Call connect first and get the ID

            // Set the ID on the connectionInfo object
            connectionInfo.setId(connectionId);

            // Save connection details to the repository with the correct ID
            connectionRepository.save(connectionInfo); // Save after getting the ID from connect

            // Test connection and get metadata using the connectionId
            String productName;
            String productVersion;
            try (Connection connection = databaseConnector.getConnection(connectionId)) { // Use the same connectionId
                productName = connection.getMetaData().getDatabaseProductName();
                productVersion = connection.getMetaData().getDatabaseProductVersion();
            }

            // Build the response using DtoMapper and then customize
            ConnectionResponse response = dtoMapper.toDto(connectionInfo);
            response.setConnectionId(connectionId); // Use the ID from the connector
            response.setDatabaseProductName(productName);
            response.setDatabaseProductVersion(productVersion);
            response.setStatus("CONNECTED");

            return ResponseEntity.ok(response);

        } catch (DatabaseConnectionException | SQLException e) {
            // If connection failed, the connector might have already logged it.
            // No need to delete from repo as it wasn't saved yet if connect() failed.
            // If save() failed after connect(), we might have a dangling connection pool entry.
            // The disconnect logic should ideally handle this. Rethrow for now.
            throw new DatabaseConnectionException("Failed to create or save connection: " + e.getMessage(), e);
        }
    }

    /**
     * Lists all active database connections.
     */
    @GetMapping
    @Operation(
        summary = "List all database connections",
        description = "Returns a list of all active database connections"
    )
    public ResponseEntity<List<ConnectionResponse>> listConnections() {
        // Get all connections from the repository
        List<DatabaseConnectionInfo> connections = connectionRepository.findAll();
        
        // Map all connections to DTOs with additional status information
        List<ConnectionResponse> responseList = new ArrayList<>();
        
        for (DatabaseConnectionInfo connectionInfo : connections) {
            ConnectionResponse response = dtoMapper.toDto(connectionInfo);
            response.setConnectionId(connectionInfo.getId());
            
            // Check if the connection is valid
            try {
                boolean isValid = databaseConnector.isConnectionValid(connectionInfo.getId());
                response.setStatus(isValid ? "AVAILABLE" : "UNAVAILABLE");
                
                // Only try to get product info if the connection is valid
                if (isValid) {
                    try (Connection connection = databaseConnector.getConnection(connectionInfo.getId())) {
                        response.setDatabaseProductName(connection.getMetaData().getDatabaseProductName());
                        response.setDatabaseProductVersion(connection.getMetaData().getDatabaseProductVersion());
                    }
                }
            } catch (Exception e) {
                response.setStatus("ERROR");
                // Log the error but don't throw it to avoid disrupting the list
                System.err.println("Error checking connection " + connectionInfo.getId() + ": " + e.getMessage());
            }
            
            responseList.add(response);
        }
        
        return ResponseEntity.ok(responseList);
    }

    /**
     * Gets details for a specific database connection.
     */
    @GetMapping("/{connectionId}")
    @Operation(
        summary = "Get connection details",
        description = "Returns details for a specific database connection"
    )
    @ApiResponse(responseCode = "200", description = "Connection found")
    @ApiResponse(responseCode = "404", description = "Connection not found")
    public ResponseEntity<ConnectionResponse> getConnection(@PathVariable UUID connectionId) {
        DatabaseConnectionInfo connectionInfo = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found: " + connectionId));
        
        // Set the ID to ensure it's available in the connection info object
        connectionInfo.setId(connectionId);
        
        // Map domain model to DTO using DtoMapper
        ConnectionResponse response = dtoMapper.toDto(connectionInfo);
        response.setConnectionId(connectionId);
        
        // Check connection and get the database product information
        try {
            boolean isValid = databaseConnector.isConnectionValid(connectionId);
            response.setStatus(isValid ? "AVAILABLE" : "UNAVAILABLE");
            
            if (isValid) {
                try (Connection connection = databaseConnector.getConnection(connectionId)) {
                    response.setDatabaseProductName(connection.getMetaData().getDatabaseProductName());
                    response.setDatabaseProductVersion(connection.getMetaData().getDatabaseProductVersion());
                }
            }
        } catch (SQLException e) {
            response.setStatus("ERROR");
            // Log the error but don't throw it since we want to return the connection info anyway
            System.err.println("Error retrieving database product information: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Gets database metadata for a specific connection.
     */
    @GetMapping("/{connectionId}/metadata")
    @Operation(
        summary = "Get database metadata",
        description = "Returns the schema information for the specified database connection"
    )
    @ApiResponse(responseCode = "200", description = "Metadata retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Connection not found")
    public ResponseEntity<SchemaInfo> getDatabaseMetadata(@PathVariable UUID connectionId) {
        // Verify connection exists before trying to get a connection object
        if (!connectionRepository.existsById(connectionId)) {
             throw new ResourceNotFoundException("Connection not found: " + connectionId);
        }

        try (Connection connection = databaseConnector.getConnection(connectionId)) {
            // Log database product name and version for debugging
            String dbType = connection.getMetaData().getDatabaseProductName();
            String dbVersion = connection.getMetaData().getDatabaseProductVersion();
            
            // Get the current catalog and schema information
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            
            // Extract metadata
            SchemaInfo schemaInfo = metadataExtractor.extractMetadata(connection);
            
            // Log some information about what was extracted
            if (schemaInfo != null) {
                int tableCount = schemaInfo.getTables() != null ? schemaInfo.getTables().size() : 0;
                int relationshipCount = 0;
                if (schemaInfo.getTables() != null) {
                    for (var table : schemaInfo.getTables()) {
                        int imported = table.getImportedRelationships() != null ? table.getImportedRelationships().size() : 0;
                        int exported = table.getExportedRelationships() != null ? table.getExportedRelationships().size() : 0;
                        relationshipCount += imported + exported;
                    }
                }
                
                // Log structured information to help with debugging
                System.out.println("Database Type: " + dbType);
                System.out.println("Database Version: " + dbVersion);
                System.out.println("Catalog: " + catalog);
                System.out.println("Schema: " + schema);
                System.out.println("Tables found: " + tableCount);
                System.out.println("Relationships found: " + relationshipCount);
            }
            
            return ResponseEntity.ok(schemaInfo);
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    /**
     * Closes a specific database connection.
     */
    @DeleteMapping("/{connectionId}")
    @Operation(
        summary = "Close a database connection",
        description = "Closes an existing database connection and releases resources"
    )
    @ApiResponse(responseCode = "204", description = "Connection closed successfully")
    @ApiResponse(responseCode = "404", description = "Connection not found")
    public ResponseEntity<Void> closeConnection(@PathVariable UUID connectionId) {
        if (!connectionRepository.existsById(connectionId)) {
            throw new ResourceNotFoundException("Connection not found: " + connectionId);
        }
        
        // First disconnect from the database to close the connection pool
        boolean disconnected = databaseConnector.disconnect(connectionId);
        
        if (!disconnected) {
            throw new DatabaseConnectionException("Failed to disconnect database connection: " + connectionId);
        }
        
        // Then delete the connection from our repository
        connectionRepository.deleteById(connectionId);
        
        return ResponseEntity.noContent().build();
    }
}