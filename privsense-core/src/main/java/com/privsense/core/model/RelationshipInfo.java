package com.privsense.core.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.UUID;

/**
 * Represents a foreign key relationship between two tables.
 * Contains information about the source and target tables/columns,
 * relationship name, and constraint details.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "relationships")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "constraintName",
    scope = RelationshipInfo.class
)
public class RelationshipInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    // Source (parent) table information
    @JsonBackReference(value = "source-table-ref")
    @ToString.Exclude // Prevent recursion
    @EqualsAndHashCode.Exclude // Prevent recursion
    @ManyToOne
    @JoinColumn(name = "source_table_id")
    private TableInfo sourceTable;
    
    @JsonBackReference(value = "source-column-ref")
    @ToString.Exclude // Prevent recursion
    @EqualsAndHashCode.Exclude // Prevent recursion
    @ManyToOne
    @JoinColumn(name = "source_column_id")
    private ColumnInfo sourceColumn;
    
    // Target (child) table information
    @JsonBackReference(value = "target-table-ref")
    @ToString.Exclude // Prevent recursion
    @EqualsAndHashCode.Exclude // Prevent recursion
    @ManyToOne
    @JoinColumn(name = "target_table_id")
    private TableInfo targetTable;
    
    @JsonBackReference(value = "target-column-ref")
    @ToString.Exclude // Prevent recursion
    @EqualsAndHashCode.Exclude // Prevent recursion
    @ManyToOne
    @JoinColumn(name = "target_column_id")
    private ColumnInfo targetColumn;
    
    // Relationship metadata
    @Column(name = "relationship_name")
    private String relationshipName;
    
    @Column(name = "constraint_name")
    private String constraintName;
    
    // Constraint behavior
    @Column(name = "update_rule")
    private String updateRule;  // e.g., "CASCADE", "RESTRICT", "SET NULL"
    
    @Column(name = "delete_rule")
    private String deleteRule;
    
    @Column(name = "\"deferrable\"")  // Quote the reserved PostgreSQL keyword
    private short deferrable;   // Is the constraint deferrable
    
    @Column(name = "initially_deferred")
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