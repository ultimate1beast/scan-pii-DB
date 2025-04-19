package com.privsense.metadata.scanner;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL-specific implementation of the DbSpecificMetadataScanner.
 * Extracts table and column comments and relationships from MySQL's information_schema.
 */
@Component
public class MySqlMetadataScanner implements DbSpecificMetadataScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(MySqlMetadataScanner.class);
    
    private static final String MYSQL_TYPE = "MySQL";
    
    @Override
    public boolean supports(String databaseType) {
        // Add more detailed logging
        logger.debug("Checking if scanner supports database type: {}", databaseType);
        boolean isSupported = MYSQL_TYPE.equalsIgnoreCase(databaseType);
        logger.debug("Database type {} is{} supported by MySqlMetadataScanner", 
            databaseType, isSupported ? "" : " not");
        return isSupported;
    }
    
    @Override
    public void enhanceSchemaInfo(Connection connection, SchemaInfo schemaInfo) throws SQLException {
        // Enhance all tables in the schema
        enhanceTables(connection, schemaInfo.getTables());
    }
    
    @Override
    public void enhanceTables(Connection connection, List<TableInfo> tables) throws SQLException {
        if (tables == null || tables.isEmpty()) {
            return;
        }
        
        String catalogName = null;
        String schemaName = null;
        
        // Get the catalog and schema name from the first table's schema
        if (tables.get(0).getSchema() != null) {
            catalogName = tables.get(0).getSchema().getCatalogName();
            schemaName = tables.get(0).getSchema().getSchemaName();
        }
        
        // For MySQL, if schema name is null, use the catalog name
        if (schemaName == null && catalogName != null) {
            schemaName = catalogName;
            logger.debug("Using catalog name as schema name: {}", schemaName);
        }
        
        // If we don't have schema info from the tables, try to get it from the connection
        if (schemaName == null) {
            try {
                schemaName = connection.getSchema();
                // In MySQL, schema name is the same as catalog name
                catalogName = schemaName;
                logger.debug("Schema name from connection: {}", schemaName);
            } catch (SQLException e) {
                // Ignore any exceptions, we'll try to proceed without schema name
                logger.warn("Could not determine schema name from connection", e);
            }
            
            // If still null, try to get catalog name
            if (schemaName == null) {
                try {
                    catalogName = connection.getCatalog();
                    schemaName = catalogName; // For MySQL, use catalog as schema
                    logger.debug("Using connection catalog as schema: {}", schemaName);
                } catch (SQLException e) {
                    logger.warn("Could not determine catalog name from connection", e);
                }
            }
        }
        
        // Final fallback - check directly in the database for the schema
        if (schemaName == null) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT DATABASE() AS current_schema")) {
                if (rs.next()) {
                    schemaName = rs.getString("current_schema");
                    logger.debug("Retrieved current schema from database: {}", schemaName);
                }
            } catch (SQLException e) {
                logger.warn("Could not determine current schema from database", e);
            }
        }
        
        enhanceTableComments(connection, catalogName, schemaName, tables);
        enhanceColumnComments(connection, catalogName, schemaName, tables);
    }
    
    @Override
    public void extractRelationships(Connection connection, SchemaInfo schemaInfo) throws SQLException {
        if (schemaInfo == null || schemaInfo.getTables() == null || schemaInfo.getTables().isEmpty()) {
            logger.warn("Cannot extract relationships: Schema or tables are null or empty");
            return;
        }
        
        String catalogName = schemaInfo.getCatalogName();
        String schemaName = schemaInfo.getSchemaName();
        
        // Log schema information for debugging
        logger.debug("Extracting relationships for catalog: {}, schema: {}", catalogName, schemaName);
        
        // For MySQL, the schema name is the same as the catalog name
        // If schema name is null, use the catalog name instead
        if (schemaName == null && catalogName != null) {
            schemaName = catalogName;
            logger.debug("Using catalog name as schema name: {}", schemaName);
        }
        
        // If schema name is still null, try to get it from connection
        if (schemaName == null) {
            try {
                schemaName = connection.getSchema();
                // In MySQL, schema name is the same as catalog name
                catalogName = schemaName;
                logger.debug("Schema name from connection: {}", schemaName);
            } catch (SQLException e) {
                logger.warn("Could not determine schema name from connection", e);
            }
            
            // If still null, try to use catalog from connection
            if (schemaName == null) {
                try {
                    catalogName = connection.getCatalog();
                    schemaName = catalogName; // For MySQL, use catalog as schema
                    logger.debug("Using connection catalog as schema: {}", schemaName);
                } catch (SQLException e) {
                    logger.warn("Could not determine catalog name from connection", e);
                }
            }
        }
        
        // Final fallback - check directly in the database for the schema
        if (schemaName == null) {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT DATABASE() AS current_schema")) {
                if (rs.next()) {
                    schemaName = rs.getString("current_schema");
                    logger.debug("Retrieved current schema from database: {}", schemaName);
                }
            } catch (SQLException e) {
                logger.warn("Could not determine current schema from database", e);
            }
        }
        
        // Log information about the tables in the schema
        if (logger.isDebugEnabled()) {
            logger.debug("Schema contains {} tables:", schemaInfo.getTables().size());
            for (TableInfo table : schemaInfo.getTables()) {
                logger.debug(" - Table: {}", table.getTableName());
            }
        }
        
        extractForeignKeys(connection, catalogName, schemaName, schemaInfo);
    }
    
    /**
     * Enhances tables with comments from information_schema.TABLES
     */
    private void enhanceTableComments(Connection connection, String catalog, String schema, List<TableInfo> tables) 
            throws SQLException {
        // MySQL stores table comments in the information_schema.TABLES view
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT " +
                     "FROM information_schema.TABLES " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME IN " +
                     generateInClause(tables.size());
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            
            // Set table names as parameters
            int paramIndex = 2;
            for (TableInfo table : tables) {
                stmt.setString(paramIndex++, table.getTableName());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String comment = rs.getString("TABLE_COMMENT");
                    
                    // Find the table and update its remarks
                    for (TableInfo table : tables) {
                        if (tableName.equalsIgnoreCase(table.getTableName())) {
                            table.setRemarks(comment);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Enhances columns with comments from information_schema.COLUMNS
     */
    private void enhanceColumnComments(Connection connection, String catalog, String schema, List<TableInfo> tables) 
            throws SQLException {
        // Create a map for quick lookup of tables by name
        Map<String, TableInfo> tableMap = new HashMap<>();
        for (TableInfo table : tables) {
            tableMap.put(table.getTableName().toLowerCase(), table);
        }
        
        // MySQL stores column comments in the information_schema.COLUMNS view
        String sql = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT " +
                     "FROM information_schema.COLUMNS " +
                     "WHERE TABLE_SCHEMA = ? AND TABLE_NAME IN " +
                     generateInClause(tables.size());
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            
            // Set table names as parameters
            int paramIndex = 2;
            for (TableInfo table : tables) {
                stmt.setString(paramIndex++, table.getTableName());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String columnName = rs.getString("COLUMN_NAME");
                    String comment = rs.getString("COLUMN_COMMENT");
                    
                    // Find the table and then the column to update its comment
                    TableInfo table = tableMap.get(tableName.toLowerCase());
                    if (table != null) {
                        for (ColumnInfo column : table.getColumns()) {
                            if (columnName.equalsIgnoreCase(column.getColumnName())) {
                                column.setComments(comment);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Extracts foreign key relationships from information_schema.KEY_COLUMN_USAGE joined with REFERENTIAL_CONSTRAINTS
     */
    private void extractForeignKeys(Connection connection, String catalog, String schema, SchemaInfo schemaInfo) 
            throws SQLException {
        // Create maps for quick lookup of tables and columns by name
        Map<String, TableInfo> tableMap = new HashMap<>();
        Map<String, Map<String, ColumnInfo>> columnMap = new HashMap<>();
        
        for (TableInfo table : schemaInfo.getTables()) {
            String tableNameLower = table.getTableName().toLowerCase();
            tableMap.put(tableNameLower, table);
            
            Map<String, ColumnInfo> tableColumns = new HashMap<>();
            for (ColumnInfo column : table.getColumns()) {
                tableColumns.put(column.getColumnName().toLowerCase(), column);
            }
            columnMap.put(tableNameLower, tableColumns);
        }
        
        // Debug log table mappings
        logger.debug("Table map contains {} entries", tableMap.size());
        
        // MySQL stores foreign key relationships in information_schema.KEY_COLUMN_USAGE and REFERENTIAL_CONSTRAINTS
        String sql = 
                "SELECT " +
                "  kcu.TABLE_NAME AS source_table, " +
                "  kcu.COLUMN_NAME AS source_column, " +
                "  kcu.REFERENCED_TABLE_NAME AS target_table, " +
                "  kcu.REFERENCED_COLUMN_NAME AS target_column, " +
                "  kcu.CONSTRAINT_NAME, " +
                "  rc.UPDATE_RULE, " +
                "  rc.DELETE_RULE " +
                "FROM information_schema.KEY_COLUMN_USAGE kcu " +
                "JOIN information_schema.REFERENTIAL_CONSTRAINTS rc " +
                "  ON kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME " +
                "  AND kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA " +
                "WHERE kcu.CONSTRAINT_SCHEMA = ? " +
                "  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL";
        
        // Log the SQL query and parameters for debugging
        logger.debug("Executing foreign key query with schema: {}", schema);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            
            logger.debug("Executing SQL: {}", sql.replace("?", "'" + schema + "'"));
            
            try (ResultSet rs = stmt.executeQuery()) {
                int relationshipCount = 0;
                
                while (rs.next()) {
                    relationshipCount++;
                    String sourceTableName = rs.getString("source_table");
                    String sourceColumnName = rs.getString("source_column");
                    String targetTableName = rs.getString("target_table");
                    String targetColumnName = rs.getString("target_column");
                    String constraintName = rs.getString("CONSTRAINT_NAME");
                    String updateRule = rs.getString("UPDATE_RULE");
                    String deleteRule = rs.getString("DELETE_RULE");
                    
                    // Log each foreign key relationship found
                    logger.debug("Found relationship: {}.{} -> {}.{} ({})", 
                            sourceTableName, sourceColumnName, 
                            targetTableName, targetColumnName, constraintName);
                    
                    // Get the tables and columns from our maps
                    TableInfo sourceTable = tableMap.get(sourceTableName.toLowerCase());
                    TableInfo targetTable = tableMap.get(targetTableName.toLowerCase());
                    
                    if (sourceTable == null || targetTable == null) {
                        logger.warn("Could not find tables for relationship: {} -> {}", 
                                sourceTableName, targetTableName);
                        continue;
                    }
                    
                    ColumnInfo sourceColumn = columnMap.get(sourceTableName.toLowerCase())
                            .get(sourceColumnName.toLowerCase());
                    ColumnInfo targetColumn = columnMap.get(targetTableName.toLowerCase())
                            .get(targetColumnName.toLowerCase());
                    
                    if (sourceColumn == null || targetColumn == null) {
                        logger.warn("Could not find columns for relationship: {}.{} -> {}.{}", 
                                sourceTableName, sourceColumnName, targetTableName, targetColumnName);
                        continue;
                    }
                    
                    // Create relationship object
                    RelationshipInfo relationship = RelationshipInfo.builder()
                            .sourceTable(targetTable)  // The PK table is the source
                            .sourceColumn(targetColumn)
                            .targetTable(sourceTable)  // The FK table is the target
                            .targetColumn(sourceColumn)
                            .relationshipName(constraintName) // Set relationshipName to be the same as constraintName
                            .constraintName(constraintName)
                            .updateRule(updateRule)
                            .deleteRule(deleteRule)
                            .build();
                    
                    // Add the relationship to both tables
                    sourceTable.addImportedRelationship(relationship); // FK side imports the relationship
                    targetTable.addExportedRelationship(relationship); // PK side exports the relationship
                    
                    logger.debug("Added relationship {} to tables {} and {}", 
                            constraintName, sourceTable.getTableName(), targetTable.getTableName());
                }
                
                logger.debug("Found {} relationships in total", relationshipCount);
                
                // If no relationships were found but we know they exist, this is suspicious
                if (relationshipCount == 0) {
                    logger.warn("No relationships found in database despite foreign keys being present in the schema");
                    
                    // Manually check if the schema/catalog name is correct
                    try (Statement checkStmt = connection.createStatement();
                         ResultSet checkRs = checkStmt.executeQuery(
                                 "SELECT TABLE_SCHEMA FROM information_schema.TABLES " +
                                 "WHERE TABLE_NAME = 'customers' LIMIT 1")) {
                        if (checkRs.next()) {
                            String actualSchema = checkRs.getString(1);
                            logger.warn("Schema '{}' was used for query, but customers table is in schema '{}'", 
                                    schema, actualSchema);
                        }
                    } catch (SQLException e) {
                        logger.error("Error checking actual schema name", e);
                    }
                }
            }
        }
    }
    
    /**
     * Generates an SQL IN clause with the specified number of placeholders
     * e.g., generateInClause(3) returns "(?, ?, ?)"
     */
    private String generateInClause(int count) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < count; i++) {
            sb.append(i > 0 ? ", ?" : "?");
        }
        sb.append(")");
        return sb.toString();
    }
}