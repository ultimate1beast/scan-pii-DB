package com.privsense.core.service;

import java.util.UUID;

/**
 * Service interface for exporting reports in different formats.
 */
public interface ReportExportService {

    /**
     * Exports a scan report as JSON.
     *
     * @param jobId The job ID
     * @return The report as a JSON byte array
     */
    byte[] exportReportAsJson(UUID jobId);

    /**
     * Exports a scan report as CSV.
     *
     * @param jobId The job ID
     * @return The report as a CSV byte array
     */
    byte[] exportReportAsCsv(UUID jobId);

    /**
     * Exports a scan report as PDF.
     *
     * @param jobId The job ID
     * @return The report as a PDF byte array
     */
    byte[] exportReportAsPdf(UUID jobId);

    /**
     * Exports a scan report as plain text.
     *
     * @param jobId The job ID
     * @return The report as a text byte array
     */
    byte[] exportReportAsText(UUID jobId);
}