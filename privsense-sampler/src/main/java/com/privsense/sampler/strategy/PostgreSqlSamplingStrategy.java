package com.privsense.sampler.strategy;

import com.privsense.core.model.ColumnInfo;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL-specific implementation of the sampling strategy.
 * Uses TABLESAMPLE or ORDER BY RANDOM() for sampling in PostgreSQL.
 */
@Component
public class PostgreSqlSamplingStrategy implements DbSpecificSamplingStrategy {

    @Override
    public String buildSamplingQuery(ColumnInfo columnInfo, int sampleSize) {
        // Format the table and column names with double quotes to handle names with spaces or reserved keywords
        String tableName = columnInfo.getTable().getTableName();
        String columnName = columnInfo.getColumnName();
        
        // PostgreSQL has multiple options for random sampling
        // For better performance on large tables, TABLESAMPLE is preferred but is approximate
        // For exact sample size, ORDER BY RANDOM() is used (but can be slower on large tables)
        
        // Using ORDER BY RANDOM() for exact sample size
        return String.format(
            "SELECT \"%s\" FROM \"%s\" ORDER BY RANDOM() LIMIT %d",
            columnName, tableName, sampleSize
        );
        
        // Alternative using TABLESAMPLE for better performance on very large tables
        // This would need to estimate a percentage that would yield approximately the desired sample size
        // String estimatedPercentage = "1"; // This would need to be calculated based on table statistics
        // return String.format(
        //     "SELECT \"%s\" FROM \"%s\" TABLESAMPLE SYSTEM (%s) LIMIT %d",
        //     columnName, tableName, estimatedPercentage, sampleSize
        // );
    }

    @Override
    public String getSupportedDatabaseName() {
        return "PostgreSQL";
    }
}