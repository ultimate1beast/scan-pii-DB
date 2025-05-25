package com.privsense.core.service;

import java.util.Map;
import java.util.UUID;

/**
 * Service interface for enhanced report export functionality.
 * Provides methods to export scan reports in various formats with customization options.
 */
public interface EnhancedReportExportService {

    /**
     * Exports a report in JSON format.
     *
     * @param jobId The scan job ID
     * @return The report as a JSON byte array
     */
    byte[] exportReportAsJson(UUID jobId);

    /**
     * Exports a report in CSV format.
     *
     * @param jobId The scan job ID
     * @return The report as a CSV byte array
     */
    byte[] exportReportAsCsv(UUID jobId);

    /**
     * Exports a report in PDF format.
     *
     * @param jobId The scan job ID
     * @return The report as a PDF byte array
     */
    byte[] exportReportAsPdf(UUID jobId);

    /**
     * Exports a report in plain text format.
     *
     * @param jobId The scan job ID
     * @return The report as a text byte array
     */
    byte[] exportReportAsText(UUID jobId);
    
    /**
     * Exports a report in HTML format.
     *
     * @param jobId The scan job ID
     * @return The report as an HTML byte array
     */
    byte[] exportReportAsHtml(UUID jobId);
    
    /**
     * Exports a report in Excel format.
     *
     * @param jobId The scan job ID
     * @return The report as an Excel byte array
     */
    byte[] exportReportAsExcel(UUID jobId);
    
    /**
     * Exports a report in the specified format with default options.
     *
     * @param jobId The scan job ID
     * @param format The format to export to (json, csv, pdf, text, html, excel)
     * @return The report in the requested format as a byte array
     */
    byte[] exportReport(UUID jobId, String format);
    
    /**
     * Exports a report in the specified format with custom options.
     *
     * @param jobId The scan job ID
     * @param format The format to export to (json, csv, pdf, text, html, excel)
     * @param options Map of formatting options:
     *                - includeMetadata: Include scan metadata (default: true)
     *                - includeSummary: Include summary information (default: true)
     *                - includePiiColumns: Include PII columns (default: true)
     *                - includeQuasiIdentifiers: Include quasi-identifiers (default: true)
     *                - includeNonPiiColumns: Include non-PII columns (default: true)
     *                - sortResults: Sort results by relevance (default: true)
     *                - prettifyOutput: Format the output for human readability (default: true)
     * @return The report in the requested format as a byte array
     */
    byte[] exportReport(UUID jobId, String format, Map<String, Object> options);
}