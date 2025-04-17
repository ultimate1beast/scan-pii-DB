package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a foreign key relationship between two tables.
 * Contains information about the source and target tables/columns,
 * relationship name, and constraint details.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RelationshipInfo {
    
    // Source (parent) table information
    private TableInfo sourceTable;
    private ColumnInfo sourceColumn;
    
    // Target (child) table information
    private TableInfo targetTable;
    private ColumnInfo targetColumn;
    
    // Relationship metadata
    private String relationshipName;
    private String constraintName;
    
    // Constraint behavior
    private String updateRule;  // e.g., "CASCADE", "RESTRICT", "SET NULL"
    private String deleteRule;
    private short deferrable;   // Is the constraint deferrable
    private short initiallyDeferred;  // Is the constraint initially deferred
    
    /**
     * Returns a descriptive string of the relationship
     */
    public String getDescription() {
        return String.format("Relationship from %s.%s to %s.%s (%s)",
                sourceTable.getTableName(), sourceColumn.getColumnName(),
                targetTable.getTableName(), targetColumn.getColumnName(),
                constraintName != null ? constraintName : "unnamed");
    }
    
    /**
     * Returns true if this relationship has cascade delete behavior
     */
    public boolean hasCascadeDelete() {
        return "CASCADE".equalsIgnoreCase(deleteRule);
    }
    
    /**
     * Returns true if this relationship has cascade update behavior
     */
    public boolean hasCascadeUpdate() {
        return "CASCADE".equalsIgnoreCase(updateRule);
    }
}