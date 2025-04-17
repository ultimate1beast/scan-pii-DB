package com.privsense.metadata.service.impl;

import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;
import com.privsense.core.service.MetadataExtractor;
import com.privsense.metadata.enhancer.DbSpecificMetadataEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the MetadataExtractor interface that uses JDBC DatabaseMetaData
 * and database-specific enhancers to extract schema information.
 */
@Service
public class JdbcMetadataExtractorImpl implements MetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(JdbcMetadataExtractorImpl.class);
    
    private final List<DbSpecificMetadataEnhancer> enhancers;
    
    @Autowired
    public JdbcMetadataExtractorImpl(List<DbSpecificMetadataEnhancer> enhancers) {
        this.enhancers = enhancers;
    }
    
    @Override
    public SchemaInfo extractMetadata(Connection connection) {
        try {
            String schemaName = connection.getSchema();
            String catalogName = connection.getCatalog();
            
            SchemaInfo schema = SchemaInfo.builder()
                    .schemaName(schemaName)
                    .catalogName(catalogName)
                    .build();
            
            // Extract tables using standard JDBC metadata
            extractTables(connection, schema);
            
            // Enhance with database-specific metadata
            enhanceSchemaInfo(connection, schema);
            
            return schema;
            
        } catch (SQLException e) {
            throw new MetadataExtractionException("Failed to extract database metadata", e);
        }
    }
    
    @Override
    public SchemaInfo extractMetadataForTables(Connection connection, List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            throw new IllegalArgumentException("Table names cannot be null or empty");
        }
        
        try {
            String schemaName = connection.getSchema();
            String catalogName = connection.getCatalog();
            
            SchemaInfo schema = SchemaInfo.builder()
                    .schemaName(schemaName)
                    .catalogName(catalogName)
                    .build();
            
            // Extract specific tables using standard JDBC metadata
            for (String tableName : tableNames) {
                extractTable(connection, schema, tableName);
            }
            
            // Enhance with database-specific metadata only for the requested tables
            DbSpecificMetadataEnhancer enhancer = getEnhancerForDatabase(connection);
            if (enhancer != null) {
                enhancer.enhanceTables(connection, schema.getTables());
                enhancer.extractRelationships(connection, schema);
            }
            
            return schema;
            
        } catch (SQLException e) {
            throw new MetadataExtractionException(
                    "Failed to extract metadata for tables: " + String.join(", ", tableNames), e);
        }
    }
    
    @Override
    public String detectDatabaseType(Connection connection) {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            
            logger.info("Detected database type: {}", productName);
            return productName;
        } catch (SQLException e) {
            throw new MetadataExtractionException("Failed to detect database type", e);
        }
    }
    
    /**
     * Extracts all tables from the database using JDBC DatabaseMetaData
     */
    private void extractTables(Connection connection, SchemaInfo schema) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        
        // Get tables (exclude system tables, only return user tables)
        try (ResultSet rs = metaData.getTables(
                schema.getCatalogName(), 
                schema.getSchemaName(), 
                null, 
                new String[]{"TABLE", "VIEW"})) {
            
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");
                String remarks = rs.getString("REMARKS");
                
                TableInfo table = TableInfo.builder()
                        .tableName(tableName)
                        .tableType(tableType)
                        .remarks(remarks)
                        .build();
                
                schema.addTable(table);
                
                // Extract columns for this table
                extractColumns(connection, table);
                
                // Extract primary keys for this table
                extractPrimaryKeys(connection, table);
            }
        }
    }
    
    /**
     * Extracts a specific table from the database using JDBC DatabaseMetaData
     */
    private void extractTable(Connection connection, SchemaInfo schema, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        
        // Get the specific table
        try (ResultSet rs = metaData.getTables(
                schema.getCatalogName(), 
                schema.getSchemaName(), 
                tableName, 
                new String[]{"TABLE", "VIEW"})) {
            
            if (rs.next()) {
                String tableType = rs.getString("TABLE_TYPE");
                String remarks = rs.getString("REMARKS");
                
                TableInfo table = TableInfo.builder()
                        .tableName(tableName)
                        .tableType(tableType)
                        .remarks(remarks)
                        .build();
                
                schema.addTable(table);
                
                // Extract columns for this table
                extractColumns(connection, table);
                
                // Extract primary keys for this table
                extractPrimaryKeys(connection, table);
            } else {
                logger.warn("Table '{}' not found", tableName);
            }
        }
    }
    
    /**
     * Extracts all columns for a table using JDBC DatabaseMetaData
     */
    private void extractColumns(Connection connection, TableInfo table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getColumns(
                table.getSchema().getCatalogName(), 
                table.getSchema().getSchemaName(), 
                table.getTableName(), 
                null)) {
            
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                int dataType = rs.getInt("DATA_TYPE");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                int nullable = rs.getInt("NULLABLE");
                String remarks = rs.getString("REMARKS");
                
                ColumnInfo column = ColumnInfo.builder()
                        .columnName(columnName)
                        .jdbcType(dataType)
                        .databaseTypeName(typeName)
                        .comments(remarks)
                        .size(columnSize)
                        .precision(columnSize)
                        .scale(decimalDigits)
                        .nullable(nullable == DatabaseMetaData.columnNullable)
                        .build();
                
                table.addColumn(column);
            }
        }
    }
    
    /**
     * Extracts primary key information for a table using JDBC DatabaseMetaData
     */
    private void extractPrimaryKeys(Connection connection, TableInfo table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        
        Map<String, ColumnInfo> columnMap = new HashMap<>();
        for (ColumnInfo column : table.getColumns()) {
            columnMap.put(column.getColumnName().toLowerCase(), column);
        }
        
        try (ResultSet rs = metaData.getPrimaryKeys(
                table.getSchema().getCatalogName(), 
                table.getSchema().getSchemaName(), 
                table.getTableName())) {
            
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                
                // Find the corresponding column and mark it as a primary key
                ColumnInfo column = columnMap.get(columnName.toLowerCase());
                if (column != null) {
                    column.setPrimaryKey(true);
                }
            }
        }
    }
    
    /**
     * Enhances the schema information with database-specific metadata
     */
    private void enhanceSchemaInfo(Connection connection, SchemaInfo schema) throws SQLException {
        DbSpecificMetadataEnhancer enhancer = getEnhancerForDatabase(connection);
        
        if (enhancer != null) {
            enhancer.enhanceSchemaInfo(connection, schema);
            enhancer.extractRelationships(connection, schema);
        } else {
            logger.warn("No specific enhancer found for database type: {}", detectDatabaseType(connection));
        }
    }
    
    /**
     * Gets the appropriate metadata enhancer for the database type
     */
    private DbSpecificMetadataEnhancer getEnhancerForDatabase(Connection connection) {
        String databaseType = detectDatabaseType(connection);
        
        for (DbSpecificMetadataEnhancer enhancer : enhancers) {
            if (enhancer.supports(databaseType)) {
                logger.debug("Found enhancer for database type: {}", databaseType);
                return enhancer;
            }
        }
        
        return null;
    }
}