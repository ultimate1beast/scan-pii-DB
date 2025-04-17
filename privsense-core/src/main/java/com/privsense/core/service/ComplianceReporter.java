package com.privsense.core.service;

import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SchemaInfo;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Defines methods for generating compliance reports based on PII detection results.
 */
public interface ComplianceReporter {
    
    /**
     * Generates a compliance report from the provided PII detection results.
     * 
     * @param scanId Unique identifier for this scan
     * @param connectionInfo Database connection information (credentials will be masked)
     * @param schema Schema information about the analyzed database
     * @param detectionResults The PII detection results
     * @param scanStartTime The time when the scan was started (milliseconds since epoch)
     * @param scanEndTime The time when the scan was completed (milliseconds since epoch)
     * @return A complete compliance report object
     */
    ComplianceReport generateReport(
            UUID scanId,
            DatabaseConnectionInfo connectionInfo,
            SchemaInfo schema,
            List<DetectionResult> detectionResults,
            long scanStartTime,
            long scanEndTime);
    
    /**
     * Exports the compliance report to JSON format.
     * 
     * @param report The compliance report to export
     * @return A JSON string representation of the report
     */
    String exportReportToJson(ComplianceReport report);
    
    /**
     * Exports the compliance report to JSON and writes it to the provided output stream.
     * 
     * @param report The compliance report to export
     * @param outputStream The stream to write the JSON to
     */
    void exportReportToJson(ComplianceReport report, OutputStream outputStream);
    
    /**
     * Exports the compliance report to a human-readable text format.
     * 
     * @param report The compliance report to export
     * @return A formatted string representation of the report
     */
    String exportReportToText(ComplianceReport report);
    
    /**
     * Exports the compliance report to a human-readable text format and writes it to the provided output stream.
     * 
     * @param report The compliance report to export
     * @param outputStream The stream to write the text to
     */
    void exportReportToText(ComplianceReport report, OutputStream outputStream);
    
    /**
     * Exports the compliance report to CSV format.
     * 
     * @param report The compliance report to export
     * @return A CSV string representation of the report
     */
    String exportReportToCsv(ComplianceReport report);
    
    /**
     * Exports the compliance report to CSV format and writes it to the provided output stream.
     * 
     * @param report The compliance report to export
     * @param outputStream The stream to write the CSV to
     */
    void exportReportToCsv(ComplianceReport report, OutputStream outputStream);
    
    /**
     * Configures the output format of the report.
     * 
     * @param formatOptions Map of format options and their values
     */
    void configureReportFormat(Map<String, Object> formatOptions);
}