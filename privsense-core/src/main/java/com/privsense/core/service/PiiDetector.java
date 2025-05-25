package com.privsense.core.service;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SampleData;

import java.util.List;
import java.util.Map;

/**
 * Interface for orchestrating the PII detection process.
 * Implementations will manage the pipeline of detection strategies.
 */
public interface PiiDetector {

    /**
     * Detects PII in a single column.
     *
     * @param columnInfo Information about the column
     * @param sampleData Sample data extracted from the column
     * @return Detection result containing PII candidates found in the column
     */
    DetectionResult detectPii(ColumnInfo columnInfo, SampleData sampleData);

    /**
     * Detects PII in multiple columns.
     *
     * @param columnDataMap Map of column info to sample data
     * @return List of detection results, one for each column
     */
    List<DetectionResult> detectPii(Map<ColumnInfo, SampleData> columnDataMap);
      /**
     * Detects PII in multiple columns with progress callback support.
     *
     * @param columnDataMap Map of column info to sample data
          * @param progressCallback Callback to report progress during detection
     * @return List of detection results, one for each column
     */
    List<DetectionResult> detectPii(Map<ColumnInfo, SampleData> columnDataMap, PiiDetectionProgressCallback progressCallback);
}