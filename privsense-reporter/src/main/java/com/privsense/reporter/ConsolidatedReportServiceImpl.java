package com.privsense.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.ScanContext;
import com.privsense.core.model.SchemaInfo;

import com.privsense.core.service.ConsolidatedReportService;
import com.privsense.core.service.ScanReportService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Implementation of ConsolidatedReportService that combines functionality from
 * multiple report generation services.
 */
@Service
@Slf4j
public class ConsolidatedReportServiceImpl implements ConsolidatedReportService {

    private final ScanReportService scanReportService;
    private final ObjectMapper objectMapper;
    
    private Map<String, Object> formatOptions = new HashMap<>();
    
    @Autowired
    public ConsolidatedReportServiceImpl(
            @Lazy ScanReportService scanReportService,
            ObjectMapper objectMapper) {
        this.scanReportService = scanReportService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ComplianceReport generateReport(ScanContext scanContext) {
        log.debug("Generating report for scan ID: {}", scanContext.getScanId());
        
        // Convert LocalDateTime to long timestamps 
        long startTimestamp = scanContext.getScanStartTime() != null ?
                scanContext.getScanStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;
        long endTimestamp = scanContext.getScanEndTime() != null ?
                scanContext.getScanEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;
        
        // Generate the report
        ComplianceReport report = generateReport(
                scanContext.getScanId(),
                scanContext.getDatabaseConnectionInfo(),
                scanContext.getSchemaInfo(),
                scanContext.getDetectionResults(),
                convertToMap(scanContext.getSamplingConfig()),
                convertToMap(scanContext.getDetectionConfig()),
                startTimestamp,
                endTimestamp,
                scanContext.getDatabaseProductName(),
                scanContext.getDatabaseProductVersion()
        );
        
        log.info("Generated report for scan ID: {} with {} PII findings", 
                scanContext.getScanId(), report.getPiiFindings().size());
        
        return report;
    }
    
    @Override
    public ComplianceReport generateReport(
            UUID scanId,
            DatabaseConnectionInfo connectionInfo,
            SchemaInfo schema,
            List<DetectionResult> detectionResults,
            Map<String, Object> samplingConfig,
            Map<String, Object> detectionConfig,
            long scanStartTime,
            long scanEndTime,
            String dbProductName,
            String dbProductVersion) {
        
        log.debug("Generating compliance report for scan ID: {}", scanId);
        
        // Separate PII findings and quasi-identifier findings
        List<DetectionResult> piiFindings = detectionResults.stream()
                .filter(DetectionResult::hasPii)
                .collect(Collectors.toList());
                
        // Include ALL columns in the report, not just PII or QI
        List<DetectionResult> reportFindings = new ArrayList<>(detectionResults);
        
        // Count quasi-identifier columns
        int qiColumnCount = (int) detectionResults.stream()
                .filter(DetectionResult::isQuasiIdentifier)
                .count();
        
        // Count QI groups by unique risk scores (each group has same risk score)
        int qiGroupCount = (int) detectionResults.stream()
                .filter(DetectionResult::isQuasiIdentifier)
                .map(DetectionResult::getQuasiIdentifierRiskScore)
                .distinct()
                .count();
        
        log.debug("Found {} PII columns and {} QI columns in {} QI groups out of {} total columns", 
                piiFindings.size(), qiColumnCount, qiGroupCount, detectionResults.size());
        
        if (samplingConfig == null) {
            samplingConfig = new HashMap<>();
        }
        
        if (detectionConfig == null) {
            detectionConfig = new HashMap<>();
        }
        
        // Configure format options with database information
        Map<String, Object> options = new HashMap<>(this.formatOptions);
        options.put("databaseProductName", dbProductName != null ? dbProductName : "Unknown");
        options.put("databaseProductVersion", dbProductVersion != null ? dbProductVersion : "Unknown");
        options.put("samplingConfig", samplingConfig);
        options.put("detectionConfig", detectionConfig);
        
        long scanDurationMillis = scanEndTime - scanStartTime;
        
        // Create the embedded summary object with quasi-identifier information
        ComplianceReport.ScanSummary summary = ComplianceReport.ScanSummary.builder()
                .tablesScanned(countTablesScanned(schema))
                .columnsScanned(detectionResults.size())
                .piiColumnsFound(piiFindings.size())
                .totalPiiCandidates(calculateTotalPiiCandidates(piiFindings))
                .quasiIdentifierColumnsFound(qiColumnCount)
                .quasiIdentifierGroupsFound(qiGroupCount)
                .scanDurationMillis(scanDurationMillis)
                .build();
        
        // Build the report with quasi-identifier information
        ComplianceReport report = ComplianceReport.builder()
                .scanId(scanId)
                .reportId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .scanStartTime(Instant.ofEpochMilli(scanStartTime))
                .scanEndTime(Instant.ofEpochMilli(scanEndTime))
                .scanDuration(Duration.between(
                        Instant.ofEpochMilli(scanStartTime), 
                        Instant.ofEpochMilli(scanEndTime)))
                .databaseHost(maskSensitiveInfo(connectionInfo.getHost()))
                .databaseName(connectionInfo.getDatabaseName())
                .databaseProductName(options.getOrDefault("databaseProductName", "Unknown").toString())
                .databaseProductVersion(options.getOrDefault("databaseProductVersion", "Unknown").toString())
                .samplingConfig(samplingConfig)
                .detectionConfig(detectionConfig)
                .totalTablesScanned(countTablesScanned(schema))
                .totalColumnsScanned(detectionResults.size())
                .totalPiiColumnsFound(piiFindings.size())
                .totalQuasiIdentifierColumnsFound(qiColumnCount)
                .summary(summary)
                .detectionResults(reportFindings) // Include ALL columns, not just PII and QI
                .build();
        
        // Establish bidirectional relationship between report and detection results
        for (DetectionResult result : detectionResults) {
            result.setReport(report);
        }
        
        log.info("Generated compliance report for scan ID: {} with {} total columns ({} PII, {} QI)", 
                scanId, reportFindings.size(), piiFindings.size(), qiColumnCount);
        
        return report;
    }

    @Override
    public String exportReportToJson(ComplianceReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (IOException e) {
            log.error("Failed to serialize report to JSON", e);
            throw new ReportGenerationException("Failed to serialize report to JSON", e);
        }
    }
    
    @Override
    public String exportReportToCsv(ComplianceReport report) {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("Table,Column,PII Type,Confidence Score,Detection Methods\n");
        
        // Data rows
        for (DetectionResult result : report.getSortedPiiFindings()) {
            csv.append(String.format("%s,%s,%s,%.2f,\"%s\"\n",
                    result.getColumnInfo().getTable().getTableName(),
                    result.getColumnInfo().getColumnName(),
                    result.getHighestConfidencePiiType(),
                    result.getHighestConfidenceScore(),
                    String.join(", ", result.getCandidates().stream()
                            .map(c -> c.getDetectionMethod())
                            .distinct()
                            .collect(Collectors.toList()))));
        }
        
        return csv.toString();
    }
    
    @Override
    public String exportReportToText(ComplianceReport report) {
        StringBuilder text = new StringBuilder();
        
        // Header information
        text.append("PrivSense Compliance Report\n");
        text.append("==========================\n\n");
        
        text.append(String.format("Scan ID: %s\n", report.getScanId()));
        text.append(String.format("Report generated: %s\n", report.getGeneratedAt()));
        text.append(String.format("Database: %s (%s %s)\n", 
                report.getDatabaseName(),
                report.getDatabaseProductName(),
                report.getDatabaseProductVersion()));
        
        // Summary information
        text.append("\nScan Summary\n");
        text.append("------------\n");
        text.append(String.format("Tables scanned: %d\n", report.getTotalTablesScanned()));
        text.append(String.format("Columns scanned: %d\n", report.getTotalColumnsScanned()));
        text.append(String.format("PII columns found: %d\n", report.getTotalPiiColumnsFound()));
        text.append(String.format("Quasi-identifier columns found: %d\n", report.getTotalQuasiIdentifierColumnsFound()));
        text.append(String.format("Scan duration: %s\n", report.getScanDuration()));
        
        // PII findings
        text.append("\nPII Findings\n");
        text.append("-----------\n");
        List<DetectionResult> sortedFindings = report.getSortedPiiFindings();
        
        for (int i = 0; i < sortedFindings.size(); i++) {
            DetectionResult result = sortedFindings.get(i);
            text.append(String.format("%d. %s.%s\n", 
                    i + 1,
                    result.getColumnInfo().getTable().getTableName(),
                    result.getColumnInfo().getColumnName()));
            text.append(String.format("   PII Type: %s\n", result.getHighestConfidencePiiType()));
            text.append(String.format("   Confidence: %.2f%%\n", result.getHighestConfidenceScore() * 100));
            text.append(String.format("   Detection Methods: %s\n", String.join(", ", result.getCandidates().stream()
                    .map(c -> c.getDetectionMethod())
                    .distinct()
                    .collect(Collectors.toList()))));
            text.append("\n");
        }
        
        return text.toString();
    }
    
    @Override
    public String exportReport(ComplianceReport report, String format) {
        switch (format.toLowerCase()) {
            case "json":
                return exportReportToJson(report);
            case "csv":
                return exportReportToCsv(report);
            case "text":
                return exportReportToText(report);
            case "pdf":
                // PDFs are binary and can't be returned as a string directly
                throw new ReportGenerationException("PDF reports cannot be returned as a string. Use exportReportAsBytes instead.");
            default:
                throw new ReportGenerationException("Unsupported report format: " + format);
        }
    }
    
    @Override
    public void exportReport(ComplianceReport report, String format, OutputStream outputStream) {
        try {
            switch (format.toLowerCase()) {
                case "json":
                    outputStream.write(exportReportToJson(report).getBytes(StandardCharsets.UTF_8));
                    break;
                case "csv":
                    outputStream.write(exportReportToCsv(report).getBytes(StandardCharsets.UTF_8));
                    break;
                case "text":
                    outputStream.write(exportReportToText(report).getBytes(StandardCharsets.UTF_8));
                    break;
                case "pdf":
                    outputStream.write(exportReportAsBytes(report, "pdf"));
                    break;
                default:
                    throw new ReportGenerationException("Unsupported report format: " + format);
            }
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to write report to output stream", e);
        }
    }
    
    @Override
    public byte[] exportReportAsBytes(ComplianceReport report, String format) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            switch (format.toLowerCase()) {
                case "json":
                    outputStream.write(exportReportToJson(report).getBytes(StandardCharsets.UTF_8));
                    break;
                case "csv":
                    outputStream.write(exportReportToCsv(report).getBytes(StandardCharsets.UTF_8));
                    break;
                case "text":
                    outputStream.write(exportReportToText(report).getBytes(StandardCharsets.UTF_8));
                    break;
                case "pdf":
                    generatePdfReport(report, outputStream);
                    break;
                default:
                    throw new ReportGenerationException("Unsupported report format: " + format);
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate report bytes", e);
        }
    }
    
    @Override
    public byte[] exportReportAsJson(UUID jobId) {
        try {
            ComplianceReport report = getScanReport(jobId);
            return exportReportToJson(report).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate JSON report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReportAsCsv(UUID jobId) {
        try {
            ComplianceReport report = getScanReport(jobId);
            return exportReportToCsv(report).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate CSV report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReportAsPdf(UUID jobId) {
        try {
            ComplianceReport report = getScanReport(jobId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            generatePdfReport(report, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReportAsText(UUID jobId) {
        try {
            ComplianceReport report = getScanReport(jobId);
            return exportReportToText(report).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ReportGenerationException("Failed to generate text report: " + e.getMessage(), e);
        }
    }

    @Override
    public void configureReportFormat(Map<String, Object> formatOptions) {
        this.formatOptions = formatOptions != null ? new HashMap<>(formatOptions) : new HashMap<>();
    }
    
    /**
     * Helper method to get a report for a scan job ID
     */
    private ComplianceReport getScanReport(UUID jobId) {
        if (scanReportService != null) {
            return scanReportService.getScanReport(jobId);
        } else {
            throw new ReportGenerationException("ScanReportService is not available");
        }
    }
    
    /**
     * Generates a PDF report from the compliance report.
     * This is a simplified implementation - in a real application, you would use
     * a PDF library like Apache PDFBox or iText.
     */
    private void generatePdfReport(ComplianceReport report, OutputStream outputStream) throws IOException {
        // In a real implementation, we would use a PDF library
        // For now, we'll just create a mock PDF structure with basic content
        outputStream.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write("1 0 obj\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write("<< /Type /Catalog /Pages 2 0 R >>\n".getBytes(StandardCharsets.UTF_8));
        outputStream.write("endobj\n".getBytes(StandardCharsets.UTF_8));
        
        // Add basic report info as comments
        String textReport = exportReportToText(report);
        outputStream.write(("% " + textReport.replace("\n", "\n% ")).getBytes(StandardCharsets.UTF_8));
        
        outputStream.write("%%EOF\n".getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Calculate the total number of PII candidates across all findings
     */
    private int calculateTotalPiiCandidates(List<DetectionResult> piiFindings) {
        int total = 0;
        for (DetectionResult result : piiFindings) {
            if (result.getCandidates() != null) {
                total += result.getCandidates().size();
            }
        }
        return total;
    }
    
    /**
     * Counts the number of tables scanned in the schema
     */
    private int countTablesScanned(SchemaInfo schema) {
        if (schema == null || schema.getTables() == null) {
            return 0;
        }
        return schema.getTables().size();
    }
    
    /**
     * Masks sensitive information in connection details
     */
    private String maskSensitiveInfo(String host) {
        if (host == null || host.isEmpty()) {
            return "Unknown";
        }
        // Keep hostname but mask any potential credentials in the URL
        if (host.contains("@")) {
            return host.substring(host.lastIndexOf('@') + 1);
        }
        return host;
    }
    
    /**
     * Converts a config object to a map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object config) {
        if (config == null) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.convertValue(config, Map.class);
        } catch (Exception e) {
            log.warn("Failed to convert config to map: {}", e.getMessage());
            return new HashMap<>();
        }
    }
}