package com.privsense.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Contains all context and results from a database scan operation.
 * Used to generate the final compliance report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanContext {

    /**
     * Information about the database connection used for the scan.
     */
    private DatabaseConnectionInfo databaseConnectionInfo;
    
    /**
     * Schema information extracted from the database.
     */
    private SchemaInfo schemaInfo;
    
    /**
     * Data samples extracted from columns.
     */
    private Map<ColumnInfo, SampleData> sampledData;
    
    /**
     * Results of the PII detection process.
     */
    private List<DetectionResult> detectionResults;
    
    /**
     * Configuration used for the sampling process.
     */
    private SamplingConfig samplingConfig;
    
    /**
     * Configuration used for the PII detection process.
     */
    private DetectionConfig detectionConfig;
    
    /**
     * Time when the scan started.
     */
    private LocalDateTime scanStartTime;
}