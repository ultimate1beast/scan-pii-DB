package com.privsense.sampler.strategy;

import com.privsense.core.model.ColumnInfo;
import org.springframework.stereotype.Component;

/**
 * SQL Server-specific implementation of the sampling strategy.
 * Uses NEWID() for random sampling in SQL Server.
 */
@Component
public class SqlServerSamplingStrategy implements DbSpecificSamplingStrategy {

    @Override
    public String buildSamplingQuery(ColumnInfo columnInfo, int sampleSize) {
        String tableName = columnInfo.getTable().getTableName();
        String columnName = columnInfo.getColumnName();
        
        // SQL Server uses TOP with ORDER BY NEWID() for random sampling
        return String.format(
            "SELECT TOP (%d) [%s] FROM [%s] ORDER BY NEWID()",
            sampleSize, columnName, tableName
        );
    }

    @Override
    public String getSupportedDatabaseName() {
        return "Microsoft SQL Server";
    }
}