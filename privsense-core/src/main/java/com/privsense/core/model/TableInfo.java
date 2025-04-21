package com.privsense.core.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a database table structure with its columns.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "tables")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "tableName"
)
public class TableInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "table_name", nullable = false)
    private String tableName;
    
    @Column(name = "table_type")
    private String tableType; // "TABLE", "VIEW", etc.
    
    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks; // Comments/descriptions for the table
    
    @Builder.Default
    @JsonManagedReference
    @ToString.Exclude // Prevent recursion in toString
    @EqualsAndHashCode.Exclude // Prevent recursion
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ColumnInfo> columns = new ArrayList<>();
    
    @Builder.Default
    @ToString.Exclude // Prevent potential recursion
    @EqualsAndHashCode.Exclude // Prevent recursion
    @OneToMany(mappedBy = "targetTable", cascade = CascadeType.ALL)
    private List<RelationshipInfo> importedRelationships = new ArrayList<>(); // Foreign keys this table references
    
    @Builder.Default
    @ToString.Exclude // Prevent potential recursion
    @EqualsAndHashCode.Exclude // Prevent recursion
    @OneToMany(mappedBy = "sourceTable", cascade = CascadeType.ALL)
    private List<RelationshipInfo> exportedRelationships = new ArrayList<>(); // Foreign keys that reference this table
    
    @JsonBackReference
    @ToString.Exclude // Prevent recursion in toString
    @EqualsAndHashCode.Exclude // Prevent recursion
    @ManyToOne
    @JoinColumn(name = "schema_id")
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