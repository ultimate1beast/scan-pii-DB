package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Types;
import java.util.UUID;

/**
 * Critical object holding column metadata information retrieved from the database.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "columns")
public class ColumnInfo {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "column_name", nullable = false)
    private String columnName;
    
    @Column(name = "jdbc_type")
    private int jdbcType; // From java.sql.Types
    
    @Column(name = "database_type_name")
    private String databaseTypeName; // Database-specific type name
    
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments; // Comments/descriptions fetched from the database
    
    @Column(name = "size")
    private Integer size; // Column size/length
    
    @Column(name = "precision")
    private Integer precision; // Decimal precision
    
    @Column(name = "scale")
    private Integer scale; // Decimal scale
    
    @Column(name = "nullable")
    private boolean nullable;
    
    @Column(name = "primary_key")
    private boolean primaryKey;
    
    @ToString.Exclude // Prevent recursion in toString
    @EqualsAndHashCode.Exclude // Prevent recursion
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "table_id")
    private TableInfo table; // Reference to the parent table with cascade settings
    
    /**
     * Returns a fully qualified name for the column (table.column format)
     */
    public String getFullyQualifiedName() {
        if (table == null) {
            return columnName;
        }
        return table.getTableName() + "." + columnName;
    }
    
    /**
     * Returns true if the column might contain string data based on the JDBC type
     */
    public boolean isStringType() {
        return jdbcType == Types.CHAR ||
               jdbcType == Types.VARCHAR ||
               jdbcType == Types.LONGVARCHAR ||
               jdbcType == Types.NCHAR ||
               jdbcType == Types.NVARCHAR ||
               jdbcType == Types.LONGNVARCHAR ||
               jdbcType == Types.CLOB ||
               jdbcType == Types.NCLOB;
    }
    
    /**
     * Returns true if the column might contain numeric data based on the JDBC type
     */
    public boolean isNumericType() {
        return jdbcType == Types.NUMERIC ||
               jdbcType == Types.DECIMAL ||
               jdbcType == Types.INTEGER ||
               jdbcType == Types.BIGINT ||
               jdbcType == Types.SMALLINT ||
               jdbcType == Types.TINYINT ||
               jdbcType == Types.FLOAT ||
               jdbcType == Types.DOUBLE ||
               jdbcType == Types.REAL;
    }
    
    /**
     * Returns true if the column might contain date/time data based on the JDBC type
     */
    public boolean isDateTimeType() {
        return jdbcType == Types.DATE ||
               jdbcType == Types.TIME ||
               jdbcType == Types.TIMESTAMP ||
               jdbcType == Types.TIME_WITH_TIMEZONE ||
               jdbcType == Types.TIMESTAMP_WITH_TIMEZONE;
    }
    
    /**
     * Returns true if the column might contain binary data based on the JDBC type
     */
    public boolean isBinaryType() {
        return jdbcType == Types.BINARY ||
               jdbcType == Types.VARBINARY ||
               jdbcType == Types.LONGVARBINARY ||
               jdbcType == Types.BLOB;
    }
}