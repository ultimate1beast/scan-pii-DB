package com.privsense.api.service.impl;


import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.api.mapper.EntityMapper;
import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.service.EnhancedReportExportService;
import com.privsense.core.service.ScanReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced implementation of the ReportExportService that provides additional export formats
 * and formatting options.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EnhancedReportExportServiceImpl implements EnhancedReportExportService {

    private final ScanReportService scanReportService;
    private final EntityMapper entityMapper;
    private final ReportExportServiceImpl basicReportExportService;

    /**
     * Default export options
     */
    private static final Map<String, Object> DEFAULT_OPTIONS = Map.of(
        "includeMetadata", true,
        "includeSummary", true,
        "includePiiColumns", true,
        "includeQuasiIdentifiers", true,
        "includeNonPiiColumns", true,
        "sortResults", true,
        "prettifyOutput", true
    );

    /**
     * Supported export formats
     */
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(Arrays.asList(
            "json", "csv", "pdf", "text", "html", "excel"));

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsJson(UUID jobId) {
        return basicReportExportService.exportReportAsJson(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsCsv(UUID jobId) {
        return basicReportExportService.exportReportAsCsv(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsPdf(UUID jobId) {
        return basicReportExportService.exportReportAsPdf(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsText(UUID jobId) {
        return basicReportExportService.exportReportAsText(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsHtml(UUID jobId) {
        try {
            log.debug("Starting HTML export for job ID: {}", jobId);
            
            // Get the report entity and convert to DTO
            ComplianceReport report = scanReportService.getScanReport(jobId);
            ComplianceReportDTO reportDto = entityMapper.toDto(report);
            
            // Separate columns into different categories for easier HTML rendering
            List<DetectionResultDTO> piiColumns = getPiiFindings(reportDto).stream()
                .filter(dto -> dto.isSensitiveData() && !dto.isQuasiIdentifier())
                .sorted(Comparator.comparing(DetectionResultDTO::getConfidenceScore).reversed())
                .collect(Collectors.toList());
                
            List<DetectionResultDTO> quasiIdentifierColumns = getPiiFindings(reportDto).stream()
                .filter(DetectionResultDTO::isQuasiIdentifier)
                .sorted(Comparator.comparing(DetectionResultDTO::getQuasiIdentifierRiskScore).reversed())
                .collect(Collectors.toList());
                
            List<DetectionResultDTO> nonPiiColumns = getAllDetectionResults(reportDto).stream()
                .filter(dto -> !dto.isSensitiveData() && !dto.isQuasiIdentifier())
                .collect(Collectors.toList());
            
            // Generate the HTML report
            StringBuilder html = new StringBuilder();
            
            // HTML header
            html.append("<!DOCTYPE html>\n<html>\n<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<title>PrivSense Scan Report - ").append(reportDto.getScanInfo().getScanId()).append("</title>\n");
            
            // Add CSS styling
            html.append("<style>\n");
            html.append("body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; }\n");
            html.append("h1, h2, h3 { color: #333; }\n");
            html.append("table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n");
            html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
            html.append("th { background-color: #f2f2f2; }\n");
            html.append("tr:nth-child(even) { background-color: #f9f9f9; }\n");
            html.append(".summary-box { background-color: #e7f3fe; border-left: 6px solid #2196F3; padding: 10px; margin-bottom: 20px; }\n");
            html.append(".pii-high { background-color: #ffcccc; }\n");
            html.append(".pii-medium { background-color: #fff2cc; }\n");
            html.append(".pii-low { background-color: #e6f3ff; }\n");
            html.append("</style>\n");
            html.append("</head>\n<body>\n");
            
            // Report header
            html.append("<h1>PrivSense Scan Report</h1>\n");
            html.append("<div class=\"summary-box\">\n");
            html.append("<p><strong>Job ID:</strong> ").append(reportDto.getScanInfo().getScanId()).append("</p>\n");
            html.append("<p><strong>Database:</strong> ").append(reportDto.getDatabaseInfo().getName()).append(" (");
            html.append(reportDto.getDatabaseInfo().getProduct()).append(")</p>\n");
            
            // Summary statistics
            html.append("<h2>Summary</h2>\n");
            html.append("<ul>\n");
            html.append("<li><strong>PII columns:</strong> ").append(piiColumns.size()).append("</li>\n");
            html.append("<li><strong>Quasi-Identifier columns:</strong> ").append(quasiIdentifierColumns.size()).append("</li>\n");
            html.append("<li><strong>Non-PII columns:</strong> ").append(nonPiiColumns.size()).append("</li>\n");
            html.append("<li><strong>Total columns scanned:</strong> ").append(reportDto.getScanSummary().getColumnsScanned()).append("</li>\n");
            
            // Calculate scan duration if available
            String scanDuration = reportDto.getScanInfo().getScanDuration();
            if (scanDuration == null || scanDuration.isEmpty()) {
                if (reportDto.getScanInfo().getScanStartTime() != null && reportDto.getScanInfo().getScanEndTime() != null) {
                    try {
                        Instant startTime = (Instant) reportDto.getScanInfo().getScanStartTime();
                        Instant endTime = (Instant) reportDto.getScanInfo().getScanEndTime();
                        long durationMillis = Duration.between(startTime, endTime).toMillis();
                        long minutes = durationMillis / (60 * 1000);
                        long seconds = (durationMillis % (60 * 1000)) / 1000;
                        scanDuration = String.format("%d minutes, %d seconds", minutes, seconds);
                    } catch (Exception e) {
                        scanDuration = "Unknown";
                    }
                } else {
                    scanDuration = "Unknown";
                }
            }
            html.append("<li><strong>Scan duration:</strong> ").append(scanDuration).append("</li>\n");
            html.append("<li><strong>Tables scanned:</strong> ").append(reportDto.getScanSummary().getTablesScanned()).append("</li>\n");
            html.append("</ul>\n");
            html.append("</div>\n");
            
            // PII columns section
            html.append("<h2>1. PII Columns</h2>\n");
            if (piiColumns.isEmpty()) {
                html.append("<p>No columns with PII were detected.</p>\n");
            } else {
                html.append("<table>\n");
                html.append("<tr><th>Table</th><th>Column</th><th>PII Type</th><th>Confidence</th><th>Data Type</th><th>Detection Methods</th></tr>\n");
                
                for (DetectionResultDTO result : piiColumns) {
                    // Determine color-coding based on confidence
                    String rowClass = "";
                    if (result.getConfidenceScore() >= 0.9) {
                        rowClass = " class=\"pii-high\"";
                    } else if (result.getConfidenceScore() >= 0.7) {
                        rowClass = " class=\"pii-medium\"";
                    } else {
                        rowClass = " class=\"pii-low\"";
                    }
                    
                    html.append("<tr").append(rowClass).append(">");
                    html.append("<td>").append(result.getTableName()).append("</td>");
                    html.append("<td>").append(result.getColumnName()).append("</td>");
                    html.append("<td>").append(result.getPiiType()).append("</td>");
                    html.append("<td>").append(String.format("%.4f", result.getConfidenceScore())).append("</td>");
                    html.append("<td>").append(result.getDataType()).append("</td>");
                    html.append("<td>").append(String.join(", ", result.getDetectionMethods())).append("</td>");
                    html.append("</tr>\n");
                }
                
                html.append("</table>\n");
            }
            
            // Quasi-Identifier columns section
            html.append("<h2>2. Quasi-Identifier Columns</h2>\n");
            if (quasiIdentifierColumns.isEmpty()) {
                html.append("<p>No quasi-identifier columns were detected.</p>\n");
            } else {
                html.append("<table>\n");
                html.append("<tr><th>Table</th><th>Column</th><th>Risk Score</th><th>Data Type</th><th>Also PII</th></tr>\n");
                
                for (DetectionResultDTO result : quasiIdentifierColumns) {
                    html.append("<tr>");
                    html.append("<td>").append(result.getTableName()).append("</td>");
                    html.append("<td>").append(result.getColumnName()).append("</td>");
                    html.append("<td>").append(String.format("%.4f", result.getQuasiIdentifierRiskScore())).append("</td>");
                    html.append("<td>").append(result.getDataType()).append("</td>");
                    html.append("<td>").append(result.isSensitiveData() ? "Yes" : "No").append("</td>");
                    html.append("</tr>\n");
                }
                
                html.append("</table>\n");
            }
            
            // Non-PII columns section (optional - can be large)
            html.append("<h2>3. Non-PII Columns</h2>\n");
            if (nonPiiColumns.isEmpty()) {
                html.append("<p>No non-PII columns with detection results were found.</p>\n");
            } else {
                html.append("<p>Total non-PII columns: ").append(nonPiiColumns.size()).append("</p>\n");
                
                // Option to toggle the display of this potentially large table
                html.append("<button onclick=\"toggleTable('nonPiiTable')\">Show/Hide Details</button>\n");
                html.append("<table id=\"nonPiiTable\" style=\"display:none;\">\n");
                html.append("<tr><th>Table</th><th>Column</th><th>Data Type</th></tr>\n");
                
                // Sort by table and then column name
                nonPiiColumns.sort(Comparator.comparing(DetectionResultDTO::getTableName)
                    .thenComparing(DetectionResultDTO::getColumnName));
                
                for (DetectionResultDTO result : nonPiiColumns) {
                    html.append("<tr>");
                    html.append("<td>").append(result.getTableName()).append("</td>");
                    html.append("<td>").append(result.getColumnName()).append("</td>");
                    html.append("<td>").append(result.getDataType()).append("</td>");
                    html.append("</tr>\n");
                }
                
                html.append("</table>\n");
            }
            
            // Recommendations section
            html.append("<h2>Recommendations</h2>\n");
            html.append("<ul>\n");
            html.append("<li>Implement data encryption for sensitive columns</li>\n");
            html.append("<li>Review access controls for PII data</li>\n");
            html.append("<li>Consider data anonymization techniques for quasi-identifiers</li>\n");
            html.append("<li>Regularly audit access to PII columns</li>\n");
            html.append("</ul>\n");
            
            // JavaScript for toggle functionality
            html.append("<script>\n");
            html.append("function toggleTable(id) {\n");
            html.append("  var table = document.getElementById(id);\n");
            html.append("  if (table.style.display === 'none') {\n");
            html.append("    table.style.display = 'table';\n");
            html.append("  } else {\n");
            html.append("    table.style.display = 'none';\n");
            html.append("  }\n");
            html.append("}\n");
            html.append("</script>\n");
            
            // Footer
            html.append("<footer>\n");
            html.append("<hr>\n");
            html.append("<p><small>Generated by PrivSense on ");
            html.append(new Date()).append("</small></p>\n");
            html.append("</footer>\n");
            
            // Close HTML tags
            html.append("</body>\n</html>");
            
            log.debug("HTML export completed successfully for job ID: {}", jobId);
            return html.toString().getBytes(StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            log.error("Failed to generate HTML report", e);
            throw new ReportGenerationException("Failed to generate HTML report: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsExcel(UUID jobId) {
        try {
            log.debug("Starting Excel export for job ID: {}", jobId);
            
            // Get the report entity and convert to DTO
            ComplianceReport report = scanReportService.getScanReport(jobId);
            ComplianceReportDTO reportDto = entityMapper.toDto(report);
            
            // For demonstration purposes, we'll create a simple CSV-style byte array
            // In a real implementation, you would use a library like Apache POI to create Excel files
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.EXCEL)) {
                // Write worksheet header - use printRecord instead of println for strings
                csvPrinter.printRecord("PrivSense Excel Report");
                csvPrinter.printRecord("Job ID: " + reportDto.getScanInfo().getScanId());
                csvPrinter.printRecord("Database: " + reportDto.getDatabaseInfo().getName());
                csvPrinter.printRecord(); // Empty row
                
                // Write PII Findings Worksheet content
                csvPrinter.printRecord("PII FINDINGS");
                csvPrinter.printRecord("Table", "Column", "PII Type", "Confidence", "Data Type", "Is QI", "Detection Methods");
                
                // Get and sort PII columns
                List<DetectionResultDTO> piiColumns = getPiiFindings(reportDto).stream()
                    .filter(dto -> dto.isSensitiveData())
                    .sorted(Comparator.comparing(DetectionResultDTO::getConfidenceScore).reversed())
                    .collect(Collectors.toList());
                
                for (DetectionResultDTO result : piiColumns) {
                    csvPrinter.printRecord(
                        result.getTableName(),
                        result.getColumnName(),
                        result.getPiiType(),
                        result.getConfidenceScore(),
                        result.getDataType(),
                        result.isQuasiIdentifier() ? "Yes" : "No",
                        String.join(", ", result.getDetectionMethods())
                    );
                }
                
                csvPrinter.printRecord(); // Empty row
                csvPrinter.printRecord("QUASI-IDENTIFIERS");
                csvPrinter.printRecord("Table", "Column", "Risk Score", "Data Type", "Is PII");
                
                // Get and sort quasi-identifier columns
                List<DetectionResultDTO> quasiIdentifierColumns = getPiiFindings(reportDto).stream()
                    .filter(DetectionResultDTO::isQuasiIdentifier)
                    .sorted(Comparator.comparing(DetectionResultDTO::getQuasiIdentifierRiskScore).reversed())
                    .collect(Collectors.toList());
                
                for (DetectionResultDTO result : quasiIdentifierColumns) {
                    csvPrinter.printRecord(
                        result.getTableName(),
                        result.getColumnName(),
                        result.getQuasiIdentifierRiskScore(),
                        result.getDataType(),
                        result.isSensitiveData() ? "Yes" : "No"
                    );
                }
                
                // In a real implementation, you would create separate worksheets for different sections
            }
            
            log.debug("Excel export completed successfully for job ID: {}", jobId);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Failed to generate Excel report", e);
            throw new ReportGenerationException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReport(UUID jobId, String format) {
        return exportReport(jobId, format, DEFAULT_OPTIONS);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReport(UUID jobId, String format, Map<String, Object> options) {
        if (format == null) {
            throw new ReportGenerationException("Export format cannot be null");
        }
        
        // Merge provided options with default options
        Map<String, Object> mergedOptions = new HashMap<>(DEFAULT_OPTIONS);
        if (options != null) {
            mergedOptions.putAll(options);
        }
        
        // Validate format
        String normalizedFormat = format.toLowerCase();
        if (!SUPPORTED_FORMATS.contains(normalizedFormat)) {
            throw new ReportGenerationException("Unsupported export format: " + format);
        }
        
        // Call the appropriate export method based on format
        switch (normalizedFormat) {
            case "json":
                return applyOptions(exportReportAsJson(jobId), mergedOptions);
            case "csv":
                return applyOptions(exportReportAsCsv(jobId), mergedOptions);
            case "pdf":
                return applyOptions(exportReportAsPdf(jobId), mergedOptions);
            case "text":
                return applyOptions(exportReportAsText(jobId), mergedOptions);
            case "html":
                return applyOptions(exportReportAsHtml(jobId), mergedOptions);
            case "excel":
                return applyOptions(exportReportAsExcel(jobId), mergedOptions);
            default:
                throw new ReportGenerationException("Unsupported export format: " + format);
        }
    }
    
    /**
     * Applies export options to the generated report data.
     * This is a simplified implementation that would need to be enhanced in a real application.
     */
    private byte[] applyOptions(byte[] reportData, Map<String, Object> options) {
        // In a real implementation, this method would apply the options to filter or format the report
        // For now, we'll just return the original data
        log.debug("Applied export options: {}", options);
        return reportData;
    }
    
    /**
     * Helper method to extract all detection results from the tableFindings structure
     */
    private List<DetectionResultDTO> getAllDetectionResults(ComplianceReportDTO reportDto) {
        if (reportDto.getTableFindings() == null) {
            return new ArrayList<>();
        }
        
        return reportDto.getTableFindings().values().stream()
                .filter(tableFinding -> tableFinding.getColumns() != null)
                .flatMap(tableFinding -> tableFinding.getColumns().stream())
                .collect(Collectors.toList());
    }
    
    /**
     * Helper method to extract only PII findings from the tableFindings structure
     */
    private List<DetectionResultDTO> getPiiFindings(ComplianceReportDTO reportDto) {
        return getAllDetectionResults(reportDto).stream()
                .filter(dto -> dto.isSensitiveData() || dto.isQuasiIdentifier())
                .collect(Collectors.toList());
    }
}