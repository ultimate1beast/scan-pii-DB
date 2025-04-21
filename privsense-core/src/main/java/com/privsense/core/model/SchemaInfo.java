package com.privsense.core.model;

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
 * Represents the database structure containing a list of tables.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "schemas")
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator.class,
    property = "schemaName",
    scope = SchemaInfo.class
)
public class SchemaInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "schema_name")
    private String schemaName;
    
    @Column(name = "catalog_name")
    private String catalogName;
    
    @Column(name = "scan_id")
    private UUID scanId;
    
    @Builder.Default
    @JsonManagedReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude // Prevent recursion in equals/hashCode
    @OneToMany(mappedBy = "schema", cascade = CascadeType.ALL, orphanRemoval = true)
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