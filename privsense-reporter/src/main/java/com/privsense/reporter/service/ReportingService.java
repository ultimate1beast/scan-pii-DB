package com.privsense.reporter.service;

import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.service.ComplianceReporter;
import com.privsense.reporter.factory.ReportGeneratorFactory;
import com.privsense.reporter.model.ScanContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service class that serves as a facade for report generation operations.
 * Simplifies the process of generating compliance reports in various formats.
 */
@Service
@RequiredArgsConstructor
public class ReportingService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);
    private final ReportGeneratorFactory reportGeneratorFactory;
    
    /**
     * Generates a compliance report from scan context information.
     * 
     * @param context The scan context containing all necessary information for the report
     * @return A compliance report object
     */
    public ComplianceReport generateReport(ScanContext context) {
        logger.debug("Generating report for scan ID: {}", context.getScanId());
        
        ComplianceReporter reporter = reportGeneratorFactory.getReporter("json");
        
        // Configure format options if available in the context
        Map<String, Object> formatOptions = Map.of(
                "samplingConfig", context.getSamplingConfig(),
                "detectionConfig", context.getDetectionConfig(),
                "databaseProductName", context.getDatabaseProductName(),
                "databaseProductVersion", context.getDatabaseProductVersion()
        );
        reporter.configureReportFormat(formatOptions);
        
        // Generate the report
        ComplianceReport report = reporter.generateReport(
                context.getScanId(),
                context.getConnectionInfo(),
                context.getSchema(),
                context.getDetectionResults(),
                context.getScanStartTime(),
                context.getScanEndTime()
        );
        
        logger.info("Generated report for scan ID: {} with {} PII findings", 
                context.getScanId(), report.getPiiFindings().size());
        
        return report;
    }
    
    /**
     * Generates a compliance report from scan data components.
     * 
     * @param scanId Unique identifier for the scan
     * @param connectionInfo Database connection information
     * @param schema Schema information about the database
     * @param detectionResults Results of the PII detection process
     * @param samplingConfig Configuration used for sampling
     * @param detectionConfig Configuration used for detection
     * @param scanStartTime Start time of the scan (Unix timestamp)
     * @param scanEndTime End time of the scan (Unix timestamp)
     * @param dbProductName Database product name
     * @param dbProductVersion Database product version
     * @return A compliance report object
     */
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
        
        ScanContext context = ScanContext.builder()
                .scanId(scanId)
                .connectionInfo(connectionInfo)
                .schema(schema)
                .detectionResults(detectionResults)
                .samplingConfig(samplingConfig)
                .detectionConfig(detectionConfig)
                .scanStartTime(scanStartTime)
                .scanEndTime(scanEndTime)
                .databaseProductName(dbProductName)
                .databaseProductVersion(dbProductVersion)
                .build();
        
        return generateReport(context);
    }
    
    /**
     * Exports a report to JSON format.
     * 
     * @param report The compliance report to export
     * @return A JSON string representation of the report
     */
    public String exportReportToJson(ComplianceReport report) {
        ComplianceReporter reporter = reportGeneratorFactory.getReporter("json");
        return reporter.exportReportToJson(report);
    }
    
    /**
     * Exports a report to JSON format and writes it to the provided output stream.
     * 
     * @param report The compliance report to export
     * @param outputStream The output stream to write to
     */
    public void exportReportToJson(ComplianceReport report, OutputStream outputStream) {
        ComplianceReporter reporter = reportGeneratorFactory.getReporter("json");
        reporter.exportReportToJson(report, outputStream);
    }
    
    /**
     * Exports a report to the specified format.
     * 
     * @param report The compliance report to export
     * @param format The format to export to (json, csv, text, etc.)
     * @return A string representation of the report in the specified format
     * @throws ReportGenerationException if the format is not supported
     */
    public String exportReport(ComplianceReport report, String format) {
        ComplianceReporter reporter = reportGeneratorFactory.getReporter(format);
        
        switch (format.toLowerCase()) {
            case "json":
                return reporter.exportReportToJson(report);
            case "csv":
                return reporter.exportReportToCsv(report);
            case "text":
                return reporter.exportReportToText(report);
            default:
                throw new ReportGenerationException("Unsupported report format: " + format);
        }
    }
    
    /**
     * Exports a report to the specified format and writes it to the provided output stream.
     * 
     * @param report The compliance report to export
     * @param format The format to export to (json, csv, text, etc.)
     * @param outputStream The output stream to write to
     * @throws ReportGenerationException if the format is not supported
     */
    public void exportReport(ComplianceReport report, String format, OutputStream outputStream) {
        ComplianceReporter reporter = reportGeneratorFactory.getReporter(format);
        
        switch (format.toLowerCase()) {
            case "json":
                reporter.exportReportToJson(report, outputStream);
                break;
            case "csv":
                reporter.exportReportToCsv(report, outputStream);
                break;
            case "text":
                reporter.exportReportToText(report, outputStream);
                break;
            default:
                throw new ReportGenerationException("Unsupported report format: " + format);
        }
    }
}