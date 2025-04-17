package com.privsense.metadata.enhancer;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.model.TableInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PostgreSQL-specific implementation of the DbSpecificMetadataEnhancer.
 * Extracts table and column comments and relationships from PostgreSQL's system catalogs.
 */
@Component
public class PostgreSqlMetadataEnhancer implements DbSpecificMetadataEnhancer {
    
    private static final Logger logger = LoggerFactory.getLogger(PostgreSqlMetadataEnhancer.class);
    
    private static final String POSTGRESQL_TYPE = "PostgreSQL";
    
    @Override
    public boolean supports(String databaseType) {
        return POSTGRESQL_TYPE.equalsIgnoreCase(databaseType);
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
        
        String schemaName = null;
        
        // Get the schema name from the first table's schema
        if (tables.get(0).getSchema() != null) {
            schemaName = tables.get(0).getSchema().getSchemaName();
        }
        
        // If we don't have schema info from the tables, try to get it from the connection
        if (schemaName == null) {
            try {
                schemaName = connection.getSchema();
            } catch (SQLException e) {
                // Ignore any exceptions, we'll try to proceed without schema name
                logger.warn("Could not determine schema name from connection", e);
                schemaName = "public"; // Default schema in PostgreSQL
            }
        }
        
        enhanceTableComments(connection, schemaName, tables);
        enhanceColumnComments(connection, schemaName, tables);
    }
    
    @Override
    public void extractRelationships(Connection connection, SchemaInfo schemaInfo) throws SQLException {
        if (schemaInfo == null || schemaInfo.getTables() == null || schemaInfo.getTables().isEmpty()) {
            return;
        }
        
        String schemaName = schemaInfo.getSchemaName();
        
        // If schema name is not available from SchemaInfo, try to get it from connection
        if (schemaName == null) {
            try {
                schemaName = connection.getSchema();
            } catch (SQLException e) {
                logger.warn("Could not determine schema name from connection", e);
                schemaName = "public"; // Default schema in PostgreSQL
            }
        }
        
        extractForeignKeys(connection, schemaName, schemaInfo);
    }
    
    /**
     * Enhances tables with comments from pg_description and pg_class
     */
    private void enhanceTableComments(Connection connection, String schema, List<TableInfo> tables) 
            throws SQLException {
        // PostgreSQL stores table comments in pg_description joined with pg_class and pg_namespace
        String sql = 
            "SELECT c.relname AS table_name, d.description " +
            "FROM pg_catalog.pg_class c " +
            "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
            "LEFT JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = 0 " +
            "WHERE n.nspname = ? AND c.relkind = 'r' AND c.relname IN " +
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
                    String tableName = rs.getString("table_name");
                    String comment = rs.getString("description");
                    
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
     * Enhances columns with comments from pg_description joined with pg_attribute and pg_class
     */
    private void enhanceColumnComments(Connection connection, String schema, List<TableInfo> tables) 
            throws SQLException {
        // Create a map for quick lookup of tables by name
        Map<String, TableInfo> tableMap = new HashMap<>();
        for (TableInfo table : tables) {
            tableMap.put(table.getTableName().toLowerCase(), table);
        }
        
        // PostgreSQL stores column comments in pg_description joined with pg_attribute, pg_class and pg_namespace
        String sql = 
            "SELECT c.relname AS table_name, a.attname AS column_name, d.description " +
            "FROM pg_catalog.pg_class c " +
            "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace " +
            "JOIN pg_catalog.pg_attribute a ON a.attrelid = c.oid " +
            "LEFT JOIN pg_catalog.pg_description d ON d.objoid = c.oid AND d.objsubid = a.attnum " +
            "WHERE n.nspname = ? AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped " +
            "AND c.relname IN " + generateInClause(tables.size());
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            
            // Set table names as parameters
            int paramIndex = 2;
            for (TableInfo table : tables) {
                stmt.setString(paramIndex++, table.getTableName());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");
                    String comment = rs.getString("description");
                    
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
     * Extracts foreign key relationships from PostgreSQL's information schema
     */
    private void extractForeignKeys(Connection connection, String schema, SchemaInfo schemaInfo) 
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
        
        // PostgreSQL stores foreign key relationships in information_schema.referential_constraints
        // and information_schema.key_column_usage
        String sql = 
            "SELECT " +
            "  kcu1.table_name AS fk_table_name, " +
            "  kcu1.column_name AS fk_column_name, " +
            "  kcu2.table_name AS pk_table_name, " +
            "  kcu2.column_name AS pk_column_name, " +
            "  rc.constraint_name, " +
            "  rc.update_rule, " +
            "  rc.delete_rule " +
            "FROM information_schema.referential_constraints rc " +
            "JOIN information_schema.key_column_usage kcu1 " +
            "  ON kcu1.constraint_catalog = rc.constraint_catalog " +
            "  AND kcu1.constraint_schema = rc.constraint_schema " +
            "  AND kcu1.constraint_name = rc.constraint_name " +
            "JOIN information_schema.key_column_usage kcu2 " +
            "  ON kcu2.constraint_catalog = rc.unique_constraint_catalog " +
            "  AND kcu2.constraint_schema = rc.unique_constraint_schema " +
            "  AND kcu2.constraint_name = rc.unique_constraint_name " +
            "  AND kcu2.ordinal_position = kcu1.ordinal_position " +
            "WHERE kcu1.table_schema = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String fkTableName = rs.getString("fk_table_name");
                    String fkColumnName = rs.getString("fk_column_name");
                    String pkTableName = rs.getString("pk_table_name");
                    String pkColumnName = rs.getString("pk_column_name");
                    String constraintName = rs.getString("constraint_name");
                    String updateRule = rs.getString("update_rule");
                    String deleteRule = rs.getString("delete_rule");
                    
                    // Get the tables and columns from our maps
                    TableInfo fkTable = tableMap.get(fkTableName.toLowerCase());
                    TableInfo pkTable = tableMap.get(pkTableName.toLowerCase());
                    
                    if (fkTable == null || pkTable == null) {
                        logger.warn("Could not find tables for relationship: {} -> {}", 
                                fkTableName, pkTableName);
                        continue;
                    }
                    
                    ColumnInfo fkColumn = columnMap.get(fkTableName.toLowerCase())
                            .get(fkColumnName.toLowerCase());
                    ColumnInfo pkColumn = columnMap.get(pkTableName.toLowerCase())
                            .get(pkColumnName.toLowerCase());
                    
                    if (fkColumn == null || pkColumn == null) {
                        logger.warn("Could not find columns for relationship: {}.{} -> {}.{}", 
                                fkTableName, fkColumnName, pkTableName, pkColumnName);
                        continue;
                    }
                    
                    // Create relationship object
                    RelationshipInfo relationship = RelationshipInfo.builder()
                            .sourceTable(pkTable)  // Primary key table is the source
                            .sourceColumn(pkColumn)
                            .targetTable(fkTable)  // Foreign key table is the target
                            .targetColumn(fkColumn)
                            .constraintName(constraintName)
                            .updateRule(updateRule)
                            .deleteRule(deleteRule)
                            .build();
                    
                    // Add the relationship to both tables
                    fkTable.addImportedRelationship(relationship);
                    pkTable.addExportedRelationship(relationship);
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