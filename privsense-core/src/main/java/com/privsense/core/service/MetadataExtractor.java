package com.privsense.core.service;

import com.privsense.core.model.SchemaInfo;

import java.sql.Connection;
import java.util.List;

/**
 * Defines methods for extracting database schema information.
 */
public interface MetadataExtractor {
    
    /**
     * Extracts full schema information from a database connection.
     * This includes tables, columns, types, comments, and relationships.
     * 
     * @param connection The active database connection
     * @return Schema information containing tables and columns
     * @throws com.privsense.core.exception.MetadataExtractionException if extraction fails
     */
    SchemaInfo extractMetadata(Connection connection);
    
    /**
     * Extracts schema information for specific tables.
     * 
     * @param connection The active database connection
     * @param tableNames List of table names to extract metadata from
     * @return Schema information containing only the specified tables and their columns
     * @throws com.privsense.core.exception.MetadataExtractionException if extraction fails
     */
    SchemaInfo extractMetadataForTables(Connection connection, List<String> tableNames);
    
    /**
     * Gets the list of column names for a specific table.
     * 
     * @param connection The active database connection
     * @param tableName The name of the table to get columns for
     * @return A list of column names
     * @throws com.privsense.core.exception.MetadataExtractionException if extraction fails
     */
    List<String> getTableColumns(Connection connection, String tableName);
    
    /**
     * Detects the database type from the connection.
     * 
     * @param connection The active database connection
     * @return A string identifying the database type (e.g., "MySQL", "PostgreSQL")
     */
    String detectDatabaseType(Connection connection);
}