package com.privsense.metadata.enhancer;

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
 * MySQL-specific implementation of the DbSpecificMetadataEnhancer.
 * Extracts table and column comments and relationships from MySQL's information_schema.
 */
@Component
public class MySqlMetadataEnhancer implements DbSpecificMetadataEnhancer {
    
    private static final Logger logger = LoggerFactory.getLogger(MySqlMetadataEnhancer.class);
    
    private static final String MYSQL_TYPE = "MySQL";
    
    @Override
    public boolean supports(String databaseType) {
        return MYSQL_TYPE.equalsIgnoreCase(databaseType);
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
        
        // If we don't have schema info from the tables, try to get it from the connection
        if (schemaName == null) {
            try {
                schemaName = connection.getSchema();
                // In MySQL, schema name is the same as catalog name
                catalogName = schemaName;
            } catch (SQLException e) {
                // Ignore any exceptions, we'll try to proceed without schema name
                logger.warn("Could not determine schema name from connection", e);
            }
        }
        
        enhanceTableComments(connection, catalogName, schemaName, tables);
        enhanceColumnComments(connection, catalogName, schemaName, tables);
    }
    
    @Override
    public void extractRelationships(Connection connection, SchemaInfo schemaInfo) throws SQLException {
        if (schemaInfo == null || schemaInfo.getTables() == null || schemaInfo.getTables().isEmpty()) {
            return;
        }
        
        String catalogName = schemaInfo.getCatalogName();
        String schemaName = schemaInfo.getSchemaName();
        
        // If catalog/schema names are not available from SchemaInfo, try to get them from connection
        if (schemaName == null) {
            try {
                schemaName = connection.getSchema();
                // In MySQL, schema name is the same as catalog name
                catalogName = schemaName;
            } catch (SQLException e) {
                logger.warn("Could not determine schema name from connection", e);
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
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sourceTableName = rs.getString("source_table");
                    String sourceColumnName = rs.getString("source_column");
                    String targetTableName = rs.getString("target_table");
                    String targetColumnName = rs.getString("target_column");
                    String constraintName = rs.getString("CONSTRAINT_NAME");
                    String updateRule = rs.getString("UPDATE_RULE");
                    String deleteRule = rs.getString("DELETE_RULE");
                    
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
                            .constraintName(constraintName)
                            .updateRule(updateRule)
                            .deleteRule(deleteRule)
                            .build();
                    
                    // Add the relationship to both tables
                    sourceTable.addImportedRelationship(relationship); // FK side imports the relationship
                    targetTable.addExportedRelationship(relationship); // PK side exports the relationship
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