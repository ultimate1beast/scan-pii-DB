package com.privsense.core.service;

import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;

import java.util.List;

/**
 * Strategy interface for detecting PII in column data.
 * Different implementations can use various approaches (heuristics, regex, NER, etc.)
 */
public interface PiiDetectionStrategy {

    /**
     * Detects potential PII in the given column and sample data.
     *
     * @param columnInfo Information about the column being analyzed
     * @param sampleData Sample data extracted from the column
     * @return A list of PII candidates detected in the column
     */
    List<PiiCandidate> detect(ColumnInfo columnInfo, SampleData sampleData);
    
    /**
     * Get the name of this detection strategy.
     * 
     * @return The strategy name (e.g., "HEURISTIC", "REGEX", "NER")
     */
    String getStrategyName();
}