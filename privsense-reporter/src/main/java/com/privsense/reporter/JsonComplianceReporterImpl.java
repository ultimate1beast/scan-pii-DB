package com.privsense.reporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.service.ComplianceReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ComplianceReporter that generates JSON format reports.
 * This is the primary implementation used for standard report generation.
 */
@Service
public class JsonComplianceReporterImpl implements ComplianceReporter {

    private static final Logger logger = LoggerFactory.getLogger(JsonComplianceReporterImpl.class);
    private final ObjectMapper objectMapper;
    private Map<String, Object> formatOptions = new HashMap<>();

    public JsonComplianceReporterImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public ComplianceReport generateReport(
            UUID scanId,
            DatabaseConnectionInfo connectionInfo,
            SchemaInfo schema,
            List<DetectionResult> detectionResults,
            long scanStartTime,
            long scanEndTime) {
        
        logger.debug("Generating compliance report for scan ID: {}", scanId);
        
        // Separate PII findings from non-PII columns
        List<DetectionResult> piiFindings = detectionResults.stream()
                .filter(DetectionResult::hasPii)
                .collect(Collectors.toList());
        
        // Extract configuration information
        Map<String, Object> samplingConfig = new HashMap<>();
        Map<String, Object> detectionConfig = new HashMap<>();
        
        // These would typically come from the actual configuration used,
        // but for simplicity we'll use placeholder values
        if (formatOptions.containsKey("samplingConfig")) {
            // Check if it's already a Map
            Object config = formatOptions.get("samplingConfig");
            if (config instanceof Map) {
                samplingConfig = (Map<String, Object>) config;
            } else {
                // Convert object to map using ObjectMapper
                try {
                    samplingConfig = objectMapper.convertValue(config, Map.class);
                } catch (Exception e) {
                    logger.warn("Failed to convert samplingConfig to map, using empty map", e);
                }
            }
        }
        
        if (formatOptions.containsKey("detectionConfig")) {
            // Check if it's already a Map
            Object config = formatOptions.get("detectionConfig");
            if (config instanceof Map) {
                detectionConfig = (Map<String, Object>) config;
            } else {
                // Convert object to map using ObjectMapper
                try {
                    detectionConfig = objectMapper.convertValue(config, Map.class);
                } catch (Exception e) {
                    logger.warn("Failed to convert detectionConfig to map, using empty map", e);
                }
            }
        }
        
        // Build the report
        ComplianceReport report = ComplianceReport.builder()
                .scanId(scanId)
                .scanStartTime(Instant.ofEpochMilli(scanStartTime))
                .scanEndTime(Instant.ofEpochMilli(scanEndTime))
                .scanDuration(Duration.between(
                        Instant.ofEpochMilli(scanStartTime), 
                        Instant.ofEpochMilli(scanEndTime)))
                .databaseHost(maskSensitiveInfo(connectionInfo.getHost()))
                .databaseName(connectionInfo.getDatabaseName())
                .databaseProductName(formatOptions.getOrDefault("databaseProductName", "Unknown").toString())
                .databaseProductVersion(formatOptions.getOrDefault("databaseProductVersion", "Unknown").toString())
                .samplingConfig(samplingConfig)
                .detectionConfig(detectionConfig)
                .totalTablesScanned(countTablesScanned(schema))
                .totalColumnsScanned(detectionResults.size())
                .totalPiiColumnsFound(piiFindings.size())
                .piiFindings(piiFindings)
                .build();
        
        logger.info("Generated compliance report for scan ID: {} with {} PII findings", 
                scanId, piiFindings.size());
        
        return report;
    }

    @Override
    public String exportReportToJson(ComplianceReport report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (IOException e) {
            logger.error("Failed to serialize report to JSON", e);
            throw new ReportGenerationException("Failed to serialize report to JSON", e);
        }
    }

    @Override
    public void exportReportToJson(ComplianceReport report, OutputStream outputStream) {
        try {
            objectMapper.writeValue(outputStream, report);
        } catch (IOException e) {
            logger.error("Failed to write report to output stream", e);
            throw new ReportGenerationException("Failed to write report to output stream", e);
        }
    }

    @Override
    public String exportReportToText(ComplianceReport report) {
        return report.getSummary() + formatPiiFindings(report);
    }

    @Override
    public void exportReportToText(ComplianceReport report, OutputStream outputStream) {
        try {
            String textReport = exportReportToText(report);
            outputStream.write(textReport.getBytes());
        } catch (IOException e) {
            logger.error("Failed to write text report to output stream", e);
            throw new ReportGenerationException("Failed to write text report to output stream", e);
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
                    String.join(", ", result.getDetectionMethods())));
        }
        
        return csv.toString();
    }

    @Override
    public void exportReportToCsv(ComplianceReport report, OutputStream outputStream) {
        try {
            String csvReport = exportReportToCsv(report);
            outputStream.write(csvReport.getBytes());
        } catch (IOException e) {
            logger.error("Failed to write CSV report to output stream", e);
            throw new ReportGenerationException("Failed to write CSV report to output stream", e);
        }
    }

    @Override
    public void configureReportFormat(Map<String, Object> formatOptions) {
        this.formatOptions = formatOptions;
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
     * Counts the number of tables scanned in the schema
     */
    private int countTablesScanned(SchemaInfo schema) {
        if (schema == null || schema.getTables() == null) {
            return 0;
        }
        return schema.getTables().size();
    }

    /**
     * Formats the PII findings for text report
     */
    private String formatPiiFindings(ComplianceReport report) {
        StringBuilder findings = new StringBuilder("\n\nDetailed PII Findings:\n");
        findings.append("=====================\n\n");
        
        List<DetectionResult> sortedFindings = report.getSortedPiiFindings();
        
        for (int i = 0; i < sortedFindings.size(); i++) {
            DetectionResult result = sortedFindings.get(i);
            findings.append(String.format("%d. %s.%s\n", 
                    i + 1,
                    result.getColumnInfo().getTable().getTableName(),
                    result.getColumnInfo().getColumnName()));
            findings.append(String.format("   PII Type: %s\n", result.getHighestConfidencePiiType()));
            findings.append(String.format("   Confidence: %.2f%%\n", result.getHighestConfidenceScore() * 100));
            findings.append(String.format("   Detection Methods: %s\n", String.join(", ", result.getDetectionMethods())));
            findings.append("\n");
        }
        
        return findings.toString();
    }
}