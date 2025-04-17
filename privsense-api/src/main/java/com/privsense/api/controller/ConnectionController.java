package com.privsense.api.controller;

import com.privsense.api.dto.ConnectionResponse;
import com.privsense.api.dto.DatabaseConnectionRequest;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.repository.ConnectionRepository;
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
import org.modelmapper.ModelMapper;
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
    private final ConnectionRepository connectionRepository;
    private final ModelMapper modelMapper;

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

        // Convert DTO to domain model using ModelMapper
        DatabaseConnectionInfo connectionInfo = modelMapper.map(request, DatabaseConnectionInfo.class);

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

            // Build the response using ModelMapper and then customize
            ConnectionResponse response = modelMapper.map(connectionInfo, ConnectionResponse.class);
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
        // In a real implementation, we would have a method to retrieve all connections
        // For now, returning an empty list as a placeholder
        return ResponseEntity.ok(new ArrayList<>());
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
        
        // Map domain model to DTO using ModelMapper
        ConnectionResponse response = modelMapper.map(connectionInfo, ConnectionResponse.class);
        response.setConnectionId(connectionId);
        response.setStatus("AVAILABLE");
                
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

        try (Connection connection = databaseConnector.getConnection(connectionId)) { // Pass UUID
            SchemaInfo schemaInfo = metadataExtractor.extractMetadata(connection);
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
        
        // Delete the connection from our repository
        connectionRepository.deleteById(connectionId);
        
        return ResponseEntity.noContent().build();
    }
}