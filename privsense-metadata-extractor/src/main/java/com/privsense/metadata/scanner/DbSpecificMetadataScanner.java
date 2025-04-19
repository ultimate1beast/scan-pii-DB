package com.privsense.metadata.scanner;

import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Interface for database-specific metadata scanning strategies.
 * Different database systems store schema metadata (especially comments and relationships)
 * in different system tables/views. This interface allows implementing database-specific
 * logic to extract and scan metadata beyond what's available via standard JDBC.
 */
public interface DbSpecificMetadataScanner {

    /**
     * Checks if this scanner supports the given database type
     * 
     * @param databaseType The database type identifier (e.g., "MySQL", "PostgreSQL")
     * @return true if this scanner can handle the given database type
     */
    boolean supports(String databaseType);
    
    /**
     * Enhances the schema information with database-specific metadata
     * (particularly comments on tables and columns)
     * 
     * @param connection The active database connection
     * @param schemaInfo The schema information to enhance (already populated with basic metadata)
     * @throws SQLException if a database error occurs
     */
    void enhanceSchemaInfo(Connection connection, SchemaInfo schemaInfo) throws SQLException;
    
    /**
     * Enhances specific tables with database-specific metadata
     * 
     * @param connection The active database connection
     * @param tables The list of tables to enhance
     * @throws SQLException if a database error occurs
     */
    void enhanceTables(Connection connection, List<TableInfo> tables) throws SQLException;
    
    /**
     * Extracts foreign key relationships between tables using database-specific queries
     * 
     * @param connection The active database connection
     * @param schemaInfo The schema information to enhance with relationships
     * @throws SQLException if a database error occurs
     */
    void extractRelationships(Connection connection, SchemaInfo schemaInfo) throws SQLException;
}