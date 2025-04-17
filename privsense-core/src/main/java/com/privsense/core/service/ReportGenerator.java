package com.privsense.core.service;

import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.ScanContext;

/**
 * Interface for generating compliance reports based on PII scan results.
 */
public interface ReportGenerator {

    /**
     * Generates a comprehensive compliance report from the scan results and context.
     *
     * @param scanContext Object containing all information about the scan including database info,
     *                   schema details, sample data, detection results, and configuration settings
     * @return A ComplianceReport containing the formatted scan results
     */
    ComplianceReport generateReport(ScanContext scanContext);
}