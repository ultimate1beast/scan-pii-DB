package com.privsense.core.service;

import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.ScanContext;
import com.privsense.core.model.SchemaInfo;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consolidated service interface for report generation and export operations.
 * Combines functionality from ReportGenerator, ComplianceReporter, and ReportExportService.
 */
public interface ConsolidatedReportService {

    /**
     * Generates a comprehensive compliance report from scan context.
     *
     * @param scanContext Object containing all information about the scan
     * @return A ComplianceReport containing the formatted scan results
     */
    ComplianceReport generateReport(ScanContext scanContext);
    
    /**
     * Generates a compliance report from the provided PII detection results and metadata.
     * 
     * @param scanId Unique identifier for this scan
     * @param connectionInfo Database connection information
     * @param schema Schema information about the analyzed database
     * @param detectionResults The PII detection results
     * @param samplingConfig Configuration used for sampling
     * @param detectionConfig Configuration used for detection
     * @param scanStartTime The time when the scan was started
     * @param scanEndTime The time when the scan was completed
     * @param dbProductName Database product name
     * @param dbProductVersion Database product version
     * @return A compliance report object
     */
    ComplianceReport generateReport(
            UUID scanId,
            DatabaseConnectionInfo connectionInfo,
            SchemaInfo schema,
            List<DetectionResult> detectionResults,
            Map<String, Object> samplingConfig,
            Map<String, Object> detectionConfig,
            long scanStartTime,
            long scanEndTime,
            String dbProductName,
            String dbProductVersion);

    /**
     * Exports a report to JSON format.
     *
     * @param report The compliance report to export
     * @return The report content as a JSON string
     */
    String exportReportToJson(ComplianceReport report);
    
    /**
     * Exports a report to CSV format.
     *
     * @param report The compliance report to export
     * @return The report content as a CSV string
     */
    String exportReportToCsv(ComplianceReport report);
    
    /**
     * Exports a report to text format.
     *
     * @param report The compliance report to export
     * @return The report content as a formatted text string
     */
    String exportReportToText(ComplianceReport report);
    
    /**
     * Exports a report in the specified format.
     *
     * @param report The compliance report to export
     * @param format The format to export to (json, csv, text, pdf)
     * @return The report content as a string in the specified format
     */
    String exportReport(ComplianceReport report, String format);
    
    /**
     * Exports a report in the specified format and writes to the provided output stream.
     *
     * @param report The compliance report to export
     * @param format The format to export to (json, csv, text, pdf)
     * @param outputStream The stream to write the output to
     */
    void exportReport(ComplianceReport report, String format, OutputStream outputStream);
    
    /**
     * Exports a report as binary data in the specified format.
     *
     * @param report The compliance report to export
     * @param format The format to export to (json, csv, text, pdf)
     * @return The report content as a byte array
     */
    byte[] exportReportAsBytes(ComplianceReport report, String format);
    
    /**
     * Exports a scan report as JSON from a job ID.
     *
     * @param jobId The job ID
     * @return The report as a JSON byte array
     */
    byte[] exportReportAsJson(UUID jobId);

    /**
     * Exports a scan report as CSV from a job ID.
     *
     * @param jobId The job ID
     * @return The report as a CSV byte array
     */
    byte[] exportReportAsCsv(UUID jobId);

    /**
     * Exports a scan report as PDF from a job ID.
     *
     * @param jobId The job ID
     * @return The report as a PDF byte array
     */
    byte[] exportReportAsPdf(UUID jobId);

    /**
     * Exports a scan report as plain text from a job ID.
     *
     * @param jobId The job ID
     * @return The report as a text byte array
     */
    byte[] exportReportAsText(UUID jobId);
    
    /**
     * Configures the output format of the report.
     * 
     * @param formatOptions Map of format options and their values
     */
    void configureReportFormat(Map<String, Object> formatOptions);
}