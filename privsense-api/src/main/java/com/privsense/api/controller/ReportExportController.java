package com.privsense.api.controller;

import com.privsense.api.service.ScanOrchestrationService;
import com.privsense.core.service.EnhancedReportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for Report Export API endpoints.
 * Provides endpoints to export reports in different formats.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Export API", description = "API endpoints for exporting reports in various formats")
public class ReportExportController {

    private final ScanOrchestrationService scanOrchestrationService;
    private final EnhancedReportExportService enhancedReportExportService;

    /**
     * Exports a report in JSON format.
     *
     * @param jobId The scan job ID
     * @return The report as a JSON file
     */
    @GetMapping("/{jobId}/export/json")
    @Operation(summary = "Export report as JSON",
            description = "Exports a scan report in JSON format with detailed information")
    public ResponseEntity<byte[]> exportReportAsJson(@PathVariable UUID jobId) {
        log.debug("REST request to export report as JSON for job ID: {}", jobId);
        byte[] report = enhancedReportExportService.exportReportAsJson(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .contentLength(report.length)
                .body(report);
    }

    /**
     * Exports a report in CSV format.
     *
     * @param jobId The scan job ID
     * @return The report as a CSV file
     */
    @GetMapping("/{jobId}/export/csv")
    @Operation(summary = "Export report as CSV",
            description = "Exports a scan report in CSV format for easy spreadsheet analysis")
    public ResponseEntity<byte[]> exportReportAsCsv(@PathVariable UUID jobId) {
        log.debug("REST request to export report as CSV for job ID: {}", jobId);
        byte[] report = enhancedReportExportService.exportReportAsCsv(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(report.length)
                .body(report);
    }

    /**
     * Exports a report in PDF format.
     *
     * @param jobId The scan job ID
     * @return The report as a PDF file
     */
    @GetMapping("/{jobId}/export/pdf")
    @Operation(summary = "Export report as PDF",
            description = "Exports a scan report in PDF format suitable for professional reporting")
    public ResponseEntity<byte[]> exportReportAsPdf(@PathVariable UUID jobId) {
        log.debug("REST request to export report as PDF for job ID: {}", jobId);
        byte[] report = enhancedReportExportService.exportReportAsPdf(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(report.length)
                .body(report);
    }

    /**
     * Exports a report in plain text format.
     *
     * @param jobId The scan job ID
     * @return The report as a text file
     */
    @GetMapping("/{jobId}/export/text")
    @Operation(summary = "Export report as plain text",
            description = "Exports a scan report in plain text format")
    public ResponseEntity<byte[]> exportReportAsText(@PathVariable UUID jobId) {
        log.debug("REST request to export report as text for job ID: {}", jobId);
        byte[] report = enhancedReportExportService.exportReportAsText(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(report.length)
                .body(report);
    }
    
    /**
     * Exports a report in HTML format.
     *
     * @param jobId The scan job ID
     * @return The report as an HTML file
     */
    @GetMapping("/{jobId}/export/html")
    @Operation(summary = "Export report as HTML",
            description = "Exports a scan report in HTML format for browser viewing")
    public ResponseEntity<byte[]> exportReportAsHtml(@PathVariable UUID jobId) {
        log.debug("REST request to export report as HTML for job ID: {}", jobId);
        byte[] report = enhancedReportExportService.exportReportAsHtml(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".html\"")
                .contentType(MediaType.TEXT_HTML)
                .contentLength(report.length)
                .body(report);
    }
    
    /**
     * Exports a report in Excel format.
     *
     * @param jobId The scan job ID
     * @return The report as an Excel file
     */
    @GetMapping("/{jobId}/export/excel")
    @Operation(summary = "Export report as Excel",
            description = "Exports a scan report in Excel format for detailed analysis")
    public ResponseEntity<byte[]> exportReportAsExcel(@PathVariable UUID jobId) {
        log.debug("REST request to export report as Excel for job ID: {}", jobId);
        byte[] report = enhancedReportExportService.exportReportAsExcel(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + jobId + ".xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(report.length)
                .body(report);
    }

    /**
     * Generic endpoint to export a report in any supported format.
     *
     * @param jobId The scan job ID
     * @param format The format (json, csv, pdf, text, html, excel)
     * @return The report in the requested format
     */
    @GetMapping("/{jobId}/export")
    @Operation(summary = "Export report in specified format",
            description = "Exports a scan report in the specified format (json, csv, pdf, text, html, excel)")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable UUID jobId,
            @RequestParam(value = "format", defaultValue = "json") String format) {
        
        log.debug("REST request to export report in {} format for job ID: {}", format, jobId);
        
        byte[] report;
        String filename = "report-" + jobId;
        MediaType mediaType;
        
        switch (format.toLowerCase()) {
            case "json":
                report = enhancedReportExportService.exportReportAsJson(jobId);
                filename += ".json";
                mediaType = MediaType.APPLICATION_JSON;
                break;
            case "csv":
                report = enhancedReportExportService.exportReportAsCsv(jobId);
                filename += ".csv";
                mediaType = MediaType.parseMediaType("text/csv");
                break;
            case "pdf":
                report = enhancedReportExportService.exportReportAsPdf(jobId);
                filename += ".pdf";
                mediaType = MediaType.APPLICATION_PDF;
                break;
            case "text":
                report = enhancedReportExportService.exportReportAsText(jobId);
                filename += ".txt";
                mediaType = MediaType.TEXT_PLAIN;
                break;
            case "html":
                report = enhancedReportExportService.exportReportAsHtml(jobId);
                filename += ".html";
                mediaType = MediaType.TEXT_HTML;
                break;
            case "excel":
                report = enhancedReportExportService.exportReportAsExcel(jobId);
                filename += ".xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            default:
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(("Unsupported format: " + format).getBytes());
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(report.length)
                .body(report);
    }
    
    /**
     * Advanced export endpoint with customization options.
     *
     * @param jobId The scan job ID
     * @param format The format (json, csv, pdf, text, html, excel)
     * @param includeMetadata Whether to include metadata in the report
     * @param includeSummary Whether to include summary information in the report
     * @param includePiiColumns Whether to include PII columns in the report
     * @param includeQuasiIdentifiers Whether to include Quasi-Identifiers in the report
     * @param includeNonPiiColumns Whether to include non-PII columns in the report
     * @param sortResults Whether to sort results by relevance
     * @param prettifyOutput Whether to format the output for human readability
     * @return The report in the requested format with applied options
     */
    @GetMapping("/{jobId}/export/custom")
    @Operation(summary = "Export report with custom options", 
               description = "Exports a scan report with customizable options for content and formatting")
    public ResponseEntity<byte[]> exportReportWithOptions(
            @PathVariable UUID jobId,
            @RequestParam(value = "format", defaultValue = "json") String format,
            @RequestParam(value = "includeMetadata", defaultValue = "true") boolean includeMetadata,
            @RequestParam(value = "includeSummary", defaultValue = "true") boolean includeSummary,
            @RequestParam(value = "includePiiColumns", defaultValue = "true") boolean includePiiColumns,
            @RequestParam(value = "includeQuasiIdentifiers", defaultValue = "true") boolean includeQuasiIdentifiers,
            @RequestParam(value = "includeNonPiiColumns", defaultValue = "true") boolean includeNonPiiColumns,
            @RequestParam(value = "sortResults", defaultValue = "true") boolean sortResults,
            @RequestParam(value = "prettifyOutput", defaultValue = "true") boolean prettifyOutput) {
        
        log.debug("REST request to export report in {} format with custom options for job ID: {}", format, jobId);
        
        // Build options map
        Map<String, Object> options = new HashMap<>();
        options.put("includeMetadata", includeMetadata);
        options.put("includeSummary", includeSummary);
        options.put("includePiiColumns", includePiiColumns);
        options.put("includeQuasiIdentifiers", includeQuasiIdentifiers);
        options.put("includeNonPiiColumns", includeNonPiiColumns);
        options.put("sortResults", sortResults);
        options.put("prettifyOutput", prettifyOutput);
        
        // Export with custom options
        byte[] report = enhancedReportExportService.exportReport(jobId, format, options);
        
        // Determine media type and filename
        String filename = "report-" + jobId;
        MediaType mediaType;
        
        switch (format.toLowerCase()) {
            case "json":
                filename += ".json";
                mediaType = MediaType.APPLICATION_JSON;
                break;
            case "csv":
                filename += ".csv";
                mediaType = MediaType.parseMediaType("text/csv");
                break;
            case "pdf":
                filename += ".pdf";
                mediaType = MediaType.APPLICATION_PDF;
                break;
            case "text":
                filename += ".txt";
                mediaType = MediaType.TEXT_PLAIN;
                break;
            case "html":
                filename += ".html";
                mediaType = MediaType.TEXT_HTML;
                break;
            case "excel":
                filename += ".xlsx";
                mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                break;
            default:
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(("Unsupported format: " + format).getBytes());
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(report.length)
                .body(report);
    }
}