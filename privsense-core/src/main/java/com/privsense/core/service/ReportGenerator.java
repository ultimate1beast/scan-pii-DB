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
    
    /**
     * Exports a report to CSV format.
     *
     * @param report The compliance report to export
     * @return The report content as CSV byte array
     */
    byte[] exportReportAsCsv(ComplianceReport report);
    
    /**
     * Exports a report to plain text format.
     *
     * @param report The compliance report to export
     * @return The report content as text byte array
     */
    byte[] exportReportAsText(ComplianceReport report);
    
    /**
     * Exports a report to PDF format.
     *
     * @param report The compliance report to export
     * @return The report content as PDF byte array
     */
    byte[] exportReportAsPdf(ComplianceReport report);
}