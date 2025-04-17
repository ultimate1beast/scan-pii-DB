package com.privsense.sampler.strategy;

import com.privsense.core.model.ColumnInfo;
import org.springframework.stereotype.Component;

/**
 * MySQL-specific implementation of the sampling strategy.
 * Uses ORDER BY RAND() for random sampling in MySQL.
 */
@Component
public class MySqlSamplingStrategy implements DbSpecificSamplingStrategy {

    @Override
    public String buildSamplingQuery(ColumnInfo columnInfo, int sampleSize) {
        // Format the table and column names with backticks to handle names with spaces or reserved keywords
        String tableName = columnInfo.getTable().getTableName();
        String columnName = columnInfo.getColumnName();
        
        // MySQL uses ORDER BY RAND() for random sampling
        return String.format(
            "SELECT `%s` FROM `%s` ORDER BY RAND() LIMIT %d",
            columnName, tableName, sampleSize
        );
    }

    @Override
    public String getSupportedDatabaseName() {
        return "MySQL";
    }
}