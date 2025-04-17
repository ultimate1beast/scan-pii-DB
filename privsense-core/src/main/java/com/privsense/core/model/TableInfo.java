package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database table structure with its columns.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableInfo {
    
    private String tableName;
    private String tableType; // "TABLE", "VIEW", etc.
    private String remarks; // Comments/descriptions for the table
    
    @Builder.Default
    private List<ColumnInfo> columns = new ArrayList<>();
    
    @Builder.Default
    private List<RelationshipInfo> importedRelationships = new ArrayList<>(); // Foreign keys this table references
    
    @Builder.Default
    private List<RelationshipInfo> exportedRelationships = new ArrayList<>(); // Foreign keys that reference this table
    
    private SchemaInfo schema; // Reference to the parent schema
    
    /**
     * Adds a column to this table and sets this table as the parent of the column
     */
    public void addColumn(ColumnInfo column) {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
        column.setTable(this);
    }
    
    /**
     * Adds an imported relationship (foreign key to another table)
     */
    public void addImportedRelationship(RelationshipInfo relationship) {
        if (importedRelationships == null) {
            importedRelationships = new ArrayList<>();
        }
        importedRelationships.add(relationship);
    }
    
    /**
     * Adds an exported relationship (foreign key from another table)
     */
    public void addExportedRelationship(RelationshipInfo relationship) {
        if (exportedRelationships == null) {
            exportedRelationships = new ArrayList<>();
        }
        exportedRelationships.add(relationship);
    }
    
    /**
     * Returns a fully qualified name for the table (schema.table format)
     */
    public String getFullyQualifiedName() {
        if (schema == null || schema.getSchemaName() == null) {
            return tableName;
        }
        return schema.getSchemaName() + "." + tableName;
    }
    
    /**
     * Returns all relationships (both imported and exported) for this table
     */
    public List<RelationshipInfo> getAllRelationships() {
        List<RelationshipInfo> allRelationships = new ArrayList<>();
        if (importedRelationships != null) {
            allRelationships.addAll(importedRelationships);
        }
        if (exportedRelationships != null) {
            allRelationships.addAll(exportedRelationships);
        }
        return allRelationships;
    }
}