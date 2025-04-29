package com.privsense.api.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.result.DetectionResultDTO;
import com.privsense.api.mapper.EntityMapper;
import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;

import com.privsense.core.service.ReportExportService;
import com.privsense.core.service.ScanJobManagementService;
import com.privsense.core.service.ScanReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for exporting reports in different formats.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {
    
    private final ScanReportService scanReportService;
    private final ScanJobManagementService scanJobManagementService;
    private final ObjectMapper objectMapper;
    private final EntityMapper entityMapper;
    
    private static final String DEFAULT_RISK_LEVEL = "Medium";

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsJson(UUID jobId) {
        try {
            log.debug("Starting JSON export for job ID: {}", jobId);
            
            // Get the report entity
            ComplianceReport report = scanReportService.getScanReport(jobId);
            
            // Force initialization of lazy collections
            initializeLazyCollections(report);
            
            // Convert to DTO to avoid circular references
            ComplianceReportDTO reportDto = entityMapper.toDto(report);
            
            // Serialize to JSON
            byte[] result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(reportDto);
            log.debug("JSON export completed successfully for job ID: {}", jobId);
            return result;
        } catch (IOException e) {
            log.error("Failed to generate JSON report", e);
            throw new ReportGenerationException("Failed to generate JSON report: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsCsv(UUID jobId) {
        try {
            log.debug("Starting CSV export for job ID: {}", jobId);
            
            // Check job status first
            scanJobManagementService.getJobStatus(jobId);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            // Setup CSV printer with headers
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("Schema", "Table", "Column", "PII Type", "Confidence", "Risk Level", "Sensitivity Level")
                    .build();

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                // Get the report with all lazy associations loaded
                ComplianceReport report = scanReportService.getScanReport(jobId);
                
                // Force initialization of lazy collections
                initializeLazyCollections(report);
                
                // Convert to DTOs after all lazy collections are initialized
                List<DetectionResultDTO> resultDtos = report.getSortedPiiFindings().stream()
                    .map(entityMapper::toDto)
                    .collect(Collectors.toList());
                
                log.debug("Found {} PII findings to export", resultDtos.size());
                
                // Write each DTO as a row in the CSV
                for (DetectionResultDTO dto : resultDtos) {
                    // Extract information from the DTO using the available fields
                    String schemaName = ""; // Schema info not directly available in DTO
                    
                    csvPrinter.printRecord(
                        schemaName,
                        dto.getTableName(),
                        dto.getColumnName(),
                        dto.getPiiType(),
                        dto.getConfidenceScore(),
                        DEFAULT_RISK_LEVEL, // Default risk level 
                        DEFAULT_RISK_LEVEL  // Default sensitivity level
                    );
                }
            }
            
            log.debug("CSV export completed successfully for job ID: {}", jobId);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error exporting to CSV format", e);
            throw new ReportGenerationException("Failed to generate CSV report: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsPdf(UUID jobId) {
        try {
            log.debug("Starting PDF export for job ID: {}", jobId);
            
            // Get the report entity
            ComplianceReport report = scanReportService.getScanReport(jobId);
            
            // Force initialization of lazy collections
            initializeLazyCollections(report);
            
            // Now convert to DTO after all lazy collections are initialized
            ComplianceReportDTO reportDto = entityMapper.toDto(report);
            
            // Mock PDF generation - in a real implementation, this would create a proper PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("1 0 obj\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("<< /Type /Catalog /Pages 2 0 R >>\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("endobj\n".getBytes(StandardCharsets.UTF_8));
            
            // Add basic report info
            outputStream.write(("PrivSense Scan Report - Job ID: " + reportDto.getScanId() + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total PII columns found: " + reportDto.getPiiFindings().size() + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Database: " + reportDto.getDatabaseName() + "\n").getBytes(StandardCharsets.UTF_8));
            
            // Finish the mock PDF
            outputStream.write("%%EOF\n".getBytes(StandardCharsets.UTF_8));
            
            log.debug("PDF export completed successfully for job ID: {}", jobId);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate PDF report", e);
            throw new ReportGenerationException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReportAsText(UUID jobId) {
        try {
            log.debug("Starting text export for job ID: {}", jobId);
            
            // Get the report entity
            ComplianceReport report = scanReportService.getScanReport(jobId);
            
            // Force initialization of lazy collections
            initializeLazyCollections(report);
            
            // Now convert to DTO after all lazy collections are initialized
            ComplianceReportDTO reportDto = entityMapper.toDto(report);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Write report header using text block
            String header = """
                    PrivSense Scan Report
                    ====================
                    
                    Job ID: %s
                    Database: %s (%s)
                    Total PII columns found: %d
                    Tables scanned: %d
                    Columns scanned: %d
                    
                    """.formatted(
                        reportDto.getScanId(),
                        reportDto.getDatabaseName(),
                        reportDto.getDatabaseProductName(),
                        reportDto.getPiiFindings().size(),
                        reportDto.getTotalTablesScanned(),
                        reportDto.getTotalColumnsScanned()
                    );
            
            outputStream.write(header.getBytes(StandardCharsets.UTF_8));
            
            // Write PII findings
            outputStream.write("PII Findings\n-----------\n\n".getBytes(StandardCharsets.UTF_8));
            
            // Use the DTOs to avoid circular references
            for (DetectionResultDTO result : reportDto.getPiiFindings()) {
                // Extract information from the DTO using the available fields
                outputStream.write(String.format(
                    "Column: %s.%s%n",
                    result.getTableName(),
                    result.getColumnName()
                ).getBytes(StandardCharsets.UTF_8));
                
                outputStream.write(String.format(
                    "PII Type: %s (Confidence: %.2f)%n",
                    result.getPiiType(),
                    result.getConfidenceScore()
                ).getBytes(StandardCharsets.UTF_8));
                
                outputStream.write(String.format(
                    "Risk Level: %s%n%n",
                    DEFAULT_RISK_LEVEL
                ).getBytes(StandardCharsets.UTF_8));
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
            
            log.debug("Text export completed successfully for job ID: {}", jobId);
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate text report", e);
            throw new ReportGenerationException("Failed to generate text report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to force initialization of lazy-loaded collections to prevent LazyInitializationException
     */
    private void initializeLazyCollections(ComplianceReport report) {
        log.debug("Initializing lazy collections for report ID: {}", report.getReportId());
        
        if (report.getDetectionResults() != null) {
            // First access the size to ensure the collection is loaded
            int size = report.getDetectionResults().size();
            log.debug("Loaded {} detection results", size);
            
            // Then access each item's lazy collections
            report.getDetectionResults().forEach(result -> {
                if (result.getCandidates() != null) {
                    // Access the size to force initialization
                    result.getCandidates().size();
                    
                    // If there's a problem with the column info, let's make sure that's loaded too
                    if (result.getColumnInfo() != null && result.getColumnInfo().getTable() != null) {
                        // Force load of the table name
                        result.getColumnInfo().getTable().getTableName();
                        
                        // If there's schema info, make sure that's loaded too
                        if (result.getColumnInfo().getTable().getSchema() != null) {
                            result.getColumnInfo().getTable().getSchema().getSchemaName();
                        }
                    }
                }
            });
        }
        
        log.debug("Completed initialization of lazy collections");
    }
}