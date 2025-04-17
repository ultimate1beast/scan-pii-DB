package com.privsense.sampler.strategy;

import com.privsense.core.model.ColumnInfo;
import org.springframework.stereotype.Component;

/**
 * Oracle-specific implementation of the sampling strategy.
 * Uses Oracle's DBMS_RANDOM.VALUE for random sampling.
 */
@Component
public class OracleSamplingStrategy implements DbSpecificSamplingStrategy {

    @Override
    public String buildSamplingQuery(ColumnInfo columnInfo, int sampleSize) {
        String tableName = columnInfo.getTable().getTableName();
        String columnName = columnInfo.getColumnName();
        
        // Oracle uses inline subquery with ORDER BY dbms_random.value
        return String.format(
            "SELECT %s FROM (SELECT %s FROM %s ORDER BY dbms_random.value) WHERE rownum <= %d",
            columnName, columnName, tableName, sampleSize
        );
    }

    @Override
    public String getSupportedDatabaseName() {
        return "Oracle";
    }
}