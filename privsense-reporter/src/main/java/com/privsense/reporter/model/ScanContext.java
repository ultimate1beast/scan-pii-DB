package com.privsense.reporter.model;

import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SampleData;
import com.privsense.core.model.SchemaInfo;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Encapsulates all the data needed to generate a compliance report.
 * This follows the Builder pattern to simplify creation of this complex object.
 */
@Data
@Builder
public class ScanContext {
    private UUID scanId;
    private DatabaseConnectionInfo connectionInfo;
    private SchemaInfo schema;
    private List<DetectionResult> detectionResults;
    private Map<String, SampleData> sampleDataMap;
    private Map<String, Object> samplingConfig;
    private Map<String, Object> detectionConfig;
    private long scanStartTime;
    private long scanEndTime;
    private String databaseProductName;
    private String databaseProductVersion;
}