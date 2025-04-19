package com.privsense.core.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Types;

/**
 * Critical object holding column metadata information retrieved from the database.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ColumnInfo {
    
    private String columnName;
    private int jdbcType; // From java.sql.Types
    private String databaseTypeName; // Database-specific type name
    private String comments; // Comments/descriptions fetched from the database
    
    private Integer size; // Column size/length
    private Integer precision; // Decimal precision
    private Integer scale; // Decimal scale
    
    private boolean nullable;
    private boolean primaryKey;
    
    @JsonBackReference
    private TableInfo table; // Reference to the parent table
    
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