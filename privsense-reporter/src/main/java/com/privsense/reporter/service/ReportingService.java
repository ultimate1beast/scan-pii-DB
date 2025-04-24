package com.privsense.reporter.service;

import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.service.ConsolidatedReportService;
import com.privsense.reporter.factory.ReportGeneratorFactory;
import com.privsense.core.model.ScanContext;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
        
        ConsolidatedReportService reporter = reportGeneratorFactory.getReporter("json");
        
        // Configure format options with database information
        Map<String, Object> formatOptions = Map.of(
                "databaseProductName", context.getDatabaseProductName(),
                "databaseProductVersion", context.getDatabaseProductVersion()
        );
        reporter.configureReportFormat(formatOptions);
        
        // Generate the report using the consolidated service
        return reporter.generateReport(context);
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
        
        // Create a core ScanContext directly
        ScanContext coreContext = ScanContext.builder()
                .scanId(scanId)
                .databaseConnectionInfo(connectionInfo)
                .schemaInfo(schema)
                .detectionResults(detectionResults)
                .samplingConfig(convertToSamplingConfig(samplingConfig))
                .detectionConfig(convertToDetectionConfig(detectionConfig))
                .scanStartTime(convertToLocalDateTime(scanStartTime))
                .scanEndTime(convertToLocalDateTime(scanEndTime))
                .databaseProductName(dbProductName)
                .databaseProductVersion(dbProductVersion)
                .build();
        
        return generateReport(coreContext);
    }
    
    /**
     * Exports a report to JSON format.
     * 
     * @param report The compliance report to export
     * @return A JSON string representation of the report
     */
    public String exportReportToJson(ComplianceReport report) {
        ConsolidatedReportService reporter = reportGeneratorFactory.getReporter("json");
        return reporter.exportReportToJson(report);
    }
    
    /**
     * Exports a report to JSON format and writes it to the provided output stream.
     * 
     * @param report The compliance report to export
     * @param outputStream The output stream to write to
     */
    public void exportReportToJson(ComplianceReport report, OutputStream outputStream) {
        ConsolidatedReportService reporter = reportGeneratorFactory.getReporter("json");
        reporter.exportReport(report, "json", outputStream);
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
        ConsolidatedReportService reporter = reportGeneratorFactory.getReporter(format);
        return reporter.exportReport(report, format);
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
        ConsolidatedReportService reporter = reportGeneratorFactory.getReporter(format);
        reporter.exportReport(report, format, outputStream);
    }
    
    /**
     * Helper method to convert epoch milliseconds to LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(long epochMillis) {
        if (epochMillis == 0) return null;
        return java.time.Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
    
    /**
     * Helper method to convert sampling config map to SamplingConfig object
     */
    private com.privsense.core.model.SamplingConfig convertToSamplingConfig(Map<String, Object> configMap) {
        if (configMap == null) return null;
        
        // Use the core model's builder directly
        com.privsense.core.model.SamplingConfig.SamplingConfigBuilder builder = 
            com.privsense.core.model.SamplingConfig.builder();
            
        // Convert map to SamplingConfig
        if (configMap.containsKey("sampleSize")) {
            builder.sampleSize(toInteger(configMap.get("sampleSize")));
        }
        if (configMap.containsKey("samplingMethod")) {
            builder.samplingMethod(configMap.get("samplingMethod").toString());
        }
        if (configMap.containsKey("maxConcurrentQueries")) {
            builder.maxConcurrentQueries(toInteger(configMap.get("maxConcurrentQueries")));
        }
        
        return builder.build();
    }
    
    /**
     * Helper method to convert detection config map to DetectionConfig object
     */
    private com.privsense.core.model.DetectionConfig convertToDetectionConfig(Map<String, Object> configMap) {
        if (configMap == null) return null;
        
        // Use the core model's builder directly
        com.privsense.core.model.DetectionConfig.DetectionConfigBuilder builder = 
            com.privsense.core.model.DetectionConfig.builder();
            
        // Convert map to DetectionConfig
        if (configMap.containsKey("heuristicThreshold")) {
            builder.heuristicThreshold(toDouble(configMap.get("heuristicThreshold")));
        }
        if (configMap.containsKey("regexThreshold")) {
            builder.regexThreshold(toDouble(configMap.get("regexThreshold")));
        }
        if (configMap.containsKey("nerThreshold")) {
            builder.nerThreshold(toDouble(configMap.get("nerThreshold")));
        }
        
        return builder.build();
    }
    
    /**
     * Helper method to convert object to Integer
     */
    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Helper method to convert object to Double
     */
    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}