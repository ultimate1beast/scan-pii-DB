package com.privsense.core.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the database structure containing a list of tables.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SchemaInfo {
    
    private String schemaName;
    private String catalogName;
    
    @Builder.Default
    @JsonManagedReference
    private List<TableInfo> tables = new ArrayList<>();
    
    /**
     * Adds a table to this schema and sets this schema as the parent of the table
     */
    public void addTable(TableInfo table) {
        if (tables == null) {
            tables = new ArrayList<>();
        }
        tables.add(table);
        table.setSchema(this);
    }
    
    /**
     * Returns the total number of columns across all tables in this schema
     */
    public int getTotalColumnCount() {
        return tables.stream()
                .mapToInt(table -> table.getColumns().size())
                .sum();
    }
    
    /**
     * Finds a table by name (case-insensitive)
     */
    public TableInfo findTableByName(String tableName) {
        if (tableName == null || tables == null) {
            return null;
        }
        return tables.stream()
                .filter(t -> tableName.equalsIgnoreCase(t.getTableName()))
                .findFirst()
                .orElse(null);
    }
}