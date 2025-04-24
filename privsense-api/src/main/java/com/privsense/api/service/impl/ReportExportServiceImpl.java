package com.privsense.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.service.ReportExportService;
import com.privsense.core.service.ScanJobManagementService;
import com.privsense.core.service.ScanPersistenceService;
import com.privsense.core.service.ScanReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Service for exporting reports in different formats.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {
    
    private final ScanReportService scanReportService;
    private final ScanPersistenceService scanPersistenceService;
    private final ScanJobManagementService scanJobManagementService;
    private final ObjectMapper objectMapper;
    
   
    private static final String DEFAULT_RISK_LEVEL = "Medium";

    @Override
    public byte[] exportReportAsJson(UUID jobId) {
        try {
            // Cast the Object return type to the expected ComplianceReportDTO type
            ComplianceReportDTO report = (ComplianceReportDTO) scanReportService.getScanReportAsDTO(jobId);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(report);
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate JSON report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReportAsCsv(UUID jobId) {
        try {
            // Check job status first
            scanJobManagementService.getJobStatus(jobId);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            // Setup CSV printer with headers
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("Schema", "Table", "Column", "PII Type", "Confidence", "Risk Level", "Sensitivity Level")
                    .build();

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                // Get all PII results for the scan
                List<DetectionResult> piiResults = scanPersistenceService.getPiiResultsByScanId(jobId);
                
                // Write each result as a row in the CSV
                for (DetectionResult result : piiResults) {
                    String schemaName = (result.getColumnInfo().getTable().getSchema() != null) ? 
                            result.getColumnInfo().getTable().getSchema().getSchemaName() : "";
                            
                    csvPrinter.printRecord(
                        schemaName,
                        result.getColumnInfo().getTable().getTableName(),
                        result.getColumnInfo().getColumnName(),
                        result.getHighestConfidencePiiType(),
                        result.getHighestConfidenceScore(),
                        DEFAULT_RISK_LEVEL, // Default risk level since getRiskLevel() doesn't exist
                        DEFAULT_RISK_LEVEL  // Default sensitivity level since getSensitivityLevel() doesn't exist
                    );
                }
            }
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate CSV report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReportAsPdf(UUID jobId) {
        try {
            // In a real implementation, we would use a PDF library like iText or Apache PDFBox
            // For now, we'll just create a placeholder PDF with basic info
            
            // Get PII results
            List<DetectionResult> piiResults = scanPersistenceService.getPiiResultsByScanId(jobId);
            
            // Mock PDF generation - in a real implementation, this would create a proper PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("1 0 obj\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("<< /Type /Catalog /Pages 2 0 R >>\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("endobj\n".getBytes(StandardCharsets.UTF_8));
            
            // Add basic report info
            outputStream.write(("PrivSense Scan Report - Job ID: " + jobId + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total PII columns found: " + piiResults.size() + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total risk score: " + 0.0 + "\n").getBytes(StandardCharsets.UTF_8)); // Default since getTotalRiskScore() doesn't exist
            
            // Finish the mock PDF
            outputStream.write("%%EOF\n".getBytes(StandardCharsets.UTF_8));
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportReportAsText(UUID jobId) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Get PII results
            List<DetectionResult> piiResults = scanPersistenceService.getPiiResultsByScanId(jobId);
            
            // Write report header using text block
            String header = """
                    PrivSense Scan Report
                    ====================
                    
                    Job ID: %s
                    Total PII columns found: %d
                    Overall risk score: %.1f
                    
                    """.formatted(jobId, piiResults.size(), 0.0);
            
            outputStream.write(header.getBytes(StandardCharsets.UTF_8));
            
            // Write PII findings
            outputStream.write("""
                    PII Findings
                    -----------
                    
                    """.getBytes(StandardCharsets.UTF_8));
            
            for (DetectionResult result : piiResults) {
                String schemaName = (result.getColumnInfo().getTable().getSchema() != null) ? 
                        result.getColumnInfo().getTable().getSchema().getSchemaName() + "." : "";
                        
                outputStream.write(String.format(
                    "Column: %s%s.%s%n",
                    schemaName,
                    result.getColumnInfo().getTable().getTableName(),
                    result.getColumnInfo().getColumnName()
                ).getBytes(StandardCharsets.UTF_8));
                
                outputStream.write(String.format(
                    "PII Type: %s (Confidence: %.2f)%n",
                    result.getHighestConfidencePiiType(),
                    result.getHighestConfidenceScore()
                ).getBytes(StandardCharsets.UTF_8));
                
                outputStream.write(String.format(
                    "Risk Level: %s%n%n",
                    DEFAULT_RISK_LEVEL // Default risk level since getRiskLevel() doesn't exist
                ).getBytes(StandardCharsets.UTF_8));
                
                // Make sure we process the detection methods if needed
                if (result.getDetectionMethods() != null) {
                    // Process detection methods if needed
                    log.debug("Detection methods: {}", result.getDetectionMethods());
                }
            }
            
            // Write recommendations using text block
            String recommendations = """
                    Recommendations
                    --------------
                    
                    - Implement data encryption for sensitive columns
                    - Review access controls for PII data
                    - Consider data anonymization techniques
                    """;
            
            outputStream.write(recommendations.getBytes(StandardCharsets.UTF_8));
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate text report: " + e.getMessage(), e);
        }
    }
}