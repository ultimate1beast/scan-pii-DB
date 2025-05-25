package com.privsense.metadata.service.impl;

import com.privsense.core.exception.MetadataExtractionException;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;
import com.privsense.core.service.MetadataExtractor;
import com.privsense.metadata.scanner.DbSpecificMetadataScanner;
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
 * and database-specific scanners to extract schema information.
 */
@Service
public class JdbcMetadataExtractorImpl implements MetadataExtractor {

    private static final Logger logger = LoggerFactory.getLogger(JdbcMetadataExtractorImpl.class);
    
    private final List<DbSpecificMetadataScanner> scanners;
    
    @Autowired
    public JdbcMetadataExtractorImpl(List<DbSpecificMetadataScanner> scanners) {
        this.scanners = scanners;
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
            DbSpecificMetadataScanner scanner = getScannerForDatabase(connection);
            if (scanner != null) {
                scanner.enhanceTables(connection, schema.getTables());
                scanner.extractRelationships(connection, schema);
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
                        .schema(schema)  // Set the schema reference to establish bidirectional relationship
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
                        .schema(schema)  // Set the schema reference to establish bidirectional relationship
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
        
        // Add debugging to trace column extraction
        logger.debug("Extracting columns for table: {}", table.getTableName());
        logger.debug("Using catalog: {}, schema: {}", 
                table.getSchema().getCatalogName(), 
                table.getSchema().getSchemaName());
        
        try (ResultSet rs = metaData.getColumns(
                table.getSchema().getCatalogName(), 
                table.getSchema().getSchemaName(), 
                table.getTableName(), 
                null)) {
            
            int columnCount = 0;
            while (rs.next()) {
                columnCount++;
                String columnName = rs.getString("COLUMN_NAME");
                int dataType = rs.getInt("DATA_TYPE");
                String typeName = rs.getString("TYPE_NAME");
                int columnSize = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                int nullable = rs.getInt("NULLABLE");
                String remarks = rs.getString("REMARKS");
                
                logger.debug("Found column: {} (type: {}) in table {}", 
                        columnName, typeName, table.getTableName());
                
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
            
            logger.debug("Extracted {} columns for table {}", columnCount, table.getTableName());
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
        DbSpecificMetadataScanner scanner = getScannerForDatabase(connection);
        
        if (scanner != null) {
            scanner.enhanceSchemaInfo(connection, schema);
            scanner.extractRelationships(connection, schema);
        } else {
            logger.warn("No specific scanner found for database type: {}", detectDatabaseType(connection));
        }
    }
    
    /**
     * Gets the appropriate metadata scanner for the database type
     */
    private DbSpecificMetadataScanner getScannerForDatabase(Connection connection) {
        String databaseType = detectDatabaseType(connection);
        
        for (DbSpecificMetadataScanner scanner : scanners) {
            if (scanner.supports(databaseType)) {
                logger.debug("Found scanner for database type: {}", databaseType);
                return scanner;
            }
        }
        
        return null;
    }

    @Override
    public List<String> getTableColumns(Connection connection, String tableName) {
        List<String> columnNames = new ArrayList<>();
        
        try {
            // Extract metadata for just this single table
            SchemaInfo schemaInfo = extractMetadataForTables(connection, List.of(tableName));
            
            // If the table was found, extract column names
            if (schemaInfo != null && !schemaInfo.getTables().isEmpty()) {
                TableInfo table = schemaInfo.getTables().get(0);
                for (ColumnInfo column : table.getColumns()) {
                    columnNames.add(column.getColumnName());
                }
                
                logger.debug("Found {} columns for table {}", columnNames.size(), tableName);
            } else {
                logger.warn("No table found with name: {}", tableName);
            }
            
            return columnNames;
            
        } catch (Exception e) {
            throw new MetadataExtractionException("Failed to get columns for table: " + tableName, e);
        }
    }
}