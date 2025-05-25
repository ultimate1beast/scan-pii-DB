package com.privsense.db.service.impl;

import com.privsense.core.repository.ConnectionRepository;
import com.privsense.core.exception.DatabaseConnectionException;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.service.DatabaseConnector;
import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.db.service.JdbcDriverLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of DatabaseConnector that manages database connections using HikariCP.
 * It maintains a registry of connection pools keyed by connection ID.
 */
@Service
public class DatabaseConnectorImpl implements DatabaseConnector {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectorImpl.class);
    
    private final JdbcDriverLoader jdbcDriverLoader;
    private final PrivSenseConfigProperties configProperties;
    private final ConnectionRepository connectionRepository;
    
    // Maps connection IDs to connection pools (kept in memory for performance)
    private final Map<UUID, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    
    @Autowired
    public DatabaseConnectorImpl(
            JdbcDriverLoader jdbcDriverLoader, 
            PrivSenseConfigProperties configProperties,
            ConnectionRepository connectionRepository) {
        this.jdbcDriverLoader = jdbcDriverLoader;
        this.configProperties = configProperties;
        this.connectionRepository = connectionRepository;
    }
    
    @Override
    public UUID connect(DatabaseConnectionInfo connectionInfo) {
        Objects.requireNonNull(connectionInfo, "Connection info cannot be null");
        
        try {
            // Load driver dynamically if specified
            if (connectionInfo.getJdbcDriverClass() != null && !connectionInfo.getJdbcDriverClass().isEmpty()) {
                boolean driverLoaded = jdbcDriverLoader.loadDriver(connectionInfo.getJdbcDriverClass());
                
                if (!driverLoaded) {
                    throw new DatabaseConnectionException(
                            "Failed to load JDBC driver: " + connectionInfo.getJdbcDriverClass());
                }
            }
            
            // Create a HikariCP configuration
            HikariConfig config = createHikariConfig(connectionInfo);
            
            // Create a new data source for this connection
            HikariDataSource dataSource = new HikariDataSource(config);
            
            // Test the connection to make sure it's valid
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) { // 5 seconds timeout
                    throw new SQLException("Connection test failed");
                }
                
                // Store additional database information for later reference
                String dbProductName = connection.getMetaData().getDatabaseProductName();
                String dbProductVersion = connection.getMetaData().getDatabaseProductVersion();
                logger.info("Connected to {} {}", dbProductName, dbProductVersion);
            }
            
            // Create a copy of the connection info without the password for security
            DatabaseConnectionInfo safeConnectionInfo = DatabaseConnectionInfo.builder()
                    .host(connectionInfo.getHost())
                    .port(connectionInfo.getPort())
                    .databaseName(connectionInfo.getDatabaseName())
                    .username(connectionInfo.getUsername())
                    .jdbcDriverClass(connectionInfo.getJdbcDriverClass())
                    .sslEnabled(connectionInfo.getSslEnabled())
                    .build();
            
            // Save the connection info in the persistent repository
            UUID connectionId = connectionRepository.save(safeConnectionInfo);
            
            // Store the data source for later retrieval
            dataSources.put(connectionId, dataSource);
            
            logger.info("Database connection established: ID={}, URL={}", 
                    connectionId, connectionInfo.buildJdbcUrl().replaceAll("password=.*?(&|$)", "password=***$1"));
            
            return connectionId;
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Failed to establish database connection: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new DatabaseConnectionException("Unexpected error during connection setup: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean disconnect(UUID connectionId) {
        if (connectionId == null) {
            return false;
        }
        
        HikariDataSource dataSource = dataSources.remove(connectionId);
        
        // Remove from the persistent repository
        connectionRepository.deleteById(connectionId);
        
        if (dataSource != null) {
            // Close the pool gracefully
            try {
                dataSource.close();
                logger.info("Database connection closed: ID={}", connectionId);
                return true;
            } catch (Exception e) {
                logger.error("Error closing database connection: ID={}", connectionId, e);
            }
        }
        
        return false;
    }
    
    @Override
    public Connection getConnection(UUID connectionId) {
        if (connectionId == null) {
            throw new DatabaseConnectionException("Connection ID cannot be null");
        }
        
        HikariDataSource dataSource = dataSources.get(connectionId);
        
        if (dataSource == null) {
            throw new DatabaseConnectionException("Connection not found: " + connectionId);
        }
        
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Failed to get connection from pool: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isConnectionValid(UUID connectionId) {
        if (connectionId == null || !dataSources.containsKey(connectionId)) {
            return false;
        }
        
        HikariDataSource dataSource = dataSources.get(connectionId);
        
        if (dataSource.isClosed()) {
            return false;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5 seconds timeout
        } catch (SQLException e) {
            logger.warn("Connection validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public DatabaseConnectionInfo getConnectionInfo(UUID connectionId) {
        if (connectionId == null) {
            throw new DatabaseConnectionException("Connection ID cannot be null");
        }
        
        return connectionRepository.findById(connectionId)
            .orElseThrow(() -> new DatabaseConnectionException("Connection not found: " + connectionId));
    }
    
    /**
     * Returns the count of active database connections.
     * 
     * @return The number of currently active database connections
     */
    @Override
    public int getActiveConnectionCount() {
        // Remove any closed datasources first to get accurate count
        dataSources.entrySet().removeIf(entry -> entry.getValue().isClosed());
        return dataSources.size();
    }
    
    /**
     * Creates a HikariConfig object from the provided DatabaseConnectionInfo
     */
    private HikariConfig createHikariConfig(DatabaseConnectionInfo connectionInfo) {
        HikariConfig config = new HikariConfig();
        
        // JDBC URL
        config.setJdbcUrl(connectionInfo.buildJdbcUrl());
        
        // Authentication
        config.setUsername(connectionInfo.getUsername());
        config.setPassword(connectionInfo.getPassword());
        
        // Pool configuration from our centralized config
        PrivSenseConfigProperties.Db.Pool pool = configProperties.getDb().getPool();
        config.setConnectionTimeout(pool.getConnectionTimeout());
        config.setIdleTimeout(pool.getIdleTimeout());
        config.setMaxLifetime(pool.getMaxLifetime());
        config.setMinimumIdle(pool.getMinimumIdle());
        config.setMaximumPoolSize(pool.getMaximumPoolSize());
        
        // SSL configuration if enabled
        if (connectionInfo.getSslEnabled() != null && connectionInfo.getSslEnabled()) {
            Properties properties = new Properties();
            properties.setProperty("useSSL", "true");
            properties.setProperty("requireSSL", "true");
            
            if (connectionInfo.getSslTrustStorePath() != null && !connectionInfo.getSslTrustStorePath().isEmpty()) {
                properties.setProperty("trustStore", connectionInfo.getSslTrustStorePath());
            }
            
            if (connectionInfo.getSslTrustStorePassword() != null && !connectionInfo.getSslTrustStorePassword().isEmpty()) {
                properties.setProperty("trustStorePassword", connectionInfo.getSslTrustStorePassword());
            }
            
            config.setDataSourceProperties(properties);
        }
        
        return config;
    }
    
    /**
     * Cleanup method called when the Spring context is being shut down.
     * Ensures all connection pools are closed gracefully.
     */
    @PreDestroy
    public void cleanupConnectionPools() {
        logger.info("Shutting down all database connection pools...");
        
        for (Map.Entry<UUID, HikariDataSource> entry : dataSources.entrySet()) {
            try {
                UUID connectionId = entry.getKey();
                HikariDataSource dataSource = entry.getValue();
                
                if (dataSource != null && !dataSource.isClosed()) {
                    dataSource.close();
                    logger.info("Closed connection pool: ID={}", connectionId);
                }
            } catch (Exception e) {
                logger.error("Error closing connection pool", e);
            }
        }
        
        dataSources.clear();
        
        logger.info("All database connection pools have been shut down");
    }
}