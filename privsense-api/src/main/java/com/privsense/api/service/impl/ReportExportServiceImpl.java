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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

            // Setup CSV printer with headers - added a Category column
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader("Category", "Table", "Column", "Data Type", "PII Type", "Confidence", "Is PII", "Is QI", "QI Risk Score", "Detection Methods")
                    .build();

            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                // Get the report with all lazy associations loaded
                ComplianceReport report = scanReportService.getScanReport(jobId);
                
                // Force initialization of lazy collections
                initializeLazyCollections(report);
                
                // Convert to DTO to avoid circular references
                ComplianceReportDTO reportDto = entityMapper.toDto(report);
                
                // Separate columns into different categories
                List<DetectionResultDTO> piiColumns = getPiiFindings(reportDto).stream()
                    .filter(dto -> dto.isSensitiveData() && !dto.isQuasiIdentifier())
                    .collect(Collectors.toList());
                    
                List<DetectionResultDTO> quasiIdentifierColumns = getPiiFindings(reportDto).stream()
                    .filter(DetectionResultDTO::isQuasiIdentifier)
                    .collect(Collectors.toList());
                    
                List<DetectionResultDTO> nonPiiColumns = getAllDetectionResults(reportDto).stream()
                    .filter(dto -> !dto.isSensitiveData() && !dto.isQuasiIdentifier())
                    .collect(Collectors.toList());
                
                log.debug("Found {} columns in total to export (PII: {}, QI: {}, Non-PII: {})", 
                    getAllDetectionResults(reportDto).size(), 
                    piiColumns.size(),
                    quasiIdentifierColumns.size(),
                    nonPiiColumns.size());
                
                // Write PII columns first
                for (DetectionResultDTO dto : piiColumns) {
                    csvPrinter.printRecord(
                        "PII",
                        dto.getTableName(),
                        dto.getColumnName(),
                        dto.getDataType(),
                        dto.getPiiType(),
                        dto.getConfidenceScore(),
                        "Yes",
                        "No",
                        dto.getQuasiIdentifierRiskScore(),
                        String.join(", ", dto.getDetectionMethods())
                    );
                }
                
                // Write Quasi-Identifier columns next
                for (DetectionResultDTO dto : quasiIdentifierColumns) {
                    csvPrinter.printRecord(
                        "Quasi-Identifier",
                        dto.getTableName(),
                        dto.getColumnName(),
                        dto.getDataType(),
                        dto.getPiiType(),
                        dto.getConfidenceScore(),
                        dto.isSensitiveData() ? "Yes" : "No",
                        "Yes",
                        dto.getQuasiIdentifierRiskScore(),
                        dto.getDetectionMethods() != null && !dto.getDetectionMethods().isEmpty() ? 
                            String.join(", ", dto.getDetectionMethods()) : "QI Analysis"
                    );
                }
                
                // Write Non-PII columns last
                for (DetectionResultDTO dto : nonPiiColumns) {
                    csvPrinter.printRecord(
                        "Non-PII",
                        dto.getTableName(),
                        dto.getColumnName(),
                        dto.getDataType(),
                        dto.getPiiType(),
                        dto.getConfidenceScore(),
                        "No",
                        "No",
                        dto.getQuasiIdentifierRiskScore(),
                        dto.getDetectionMethods() != null && !dto.getDetectionMethods().isEmpty() ? 
                            String.join(", ", dto.getDetectionMethods()) : ""
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
            
            // Calculate scan duration if it's empty in the DTO
            String scanDuration = reportDto.getScanInfo().getScanDuration();
            if (scanDuration == null || scanDuration.isEmpty()) {
                if (reportDto.getScanInfo().getScanStartTime() != null && reportDto.getScanInfo().getScanEndTime() != null) {
                    try {
                        // Get the Instant objects with proper casting from Object
                        Instant startTime = (Instant) reportDto.getScanInfo().getScanStartTime();
                        Instant endTime = (Instant) reportDto.getScanInfo().getScanEndTime();
                        long durationMillis = Duration.between(startTime, endTime).toMillis();
                        long minutes = durationMillis / (60 * 1000);
                        long seconds = (durationMillis % (60 * 1000)) / 1000;
                        scanDuration = String.format("%d minutes, %d seconds", minutes, seconds);
                        // Update the DTO with calculated duration
                        reportDto.getScanInfo().setScanDuration(scanDuration);
                    } catch (Exception e) {
                        log.warn("Failed to calculate scan duration: {}", e.getMessage());
                        scanDuration = "Unknown";
                    }
                } else {
                    scanDuration = "Unknown";
                }
            }
            
            // Count quasi-identifiers
            long qiCount = getPiiFindings(reportDto).stream()
                .filter(DetectionResultDTO::isQuasiIdentifier)
                .count();
                
            // Count PII columns excluding quasi-identifiers
            long piiCount = getPiiFindings(reportDto).stream()
                .filter(dto -> !dto.isQuasiIdentifier() && dto.isSensitiveData())
                .count();
                
            // Count non-PII columns
            long nonPiiCount = getAllDetectionResults(reportDto).size() - getPiiFindings(reportDto).size();
                
            // Mock PDF generation - in a real implementation, this would create a proper PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write("%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("1 0 obj\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("<< /Type /Catalog /Pages 2 0 R >>\n".getBytes(StandardCharsets.UTF_8));
            outputStream.write("endobj\n".getBytes(StandardCharsets.UTF_8));
            
            // Add basic report info using the nested structure
            outputStream.write(("PrivSense Scan Report - Job ID: " + reportDto.getScanInfo().getScanId() + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Database: " + reportDto.getDatabaseInfo().getName() + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total columns scanned: " + getAllDetectionResults(reportDto).size() + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total PII columns found: " + piiCount + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total Quasi-Identifier columns: " + qiCount + "\n").getBytes(StandardCharsets.UTF_8));
            outputStream.write(("Total non-PII columns: " + nonPiiCount + "\n").getBytes(StandardCharsets.UTF_8));
            
            // Add column details
            outputStream.write("\nColumn Details:\n".getBytes(StandardCharsets.UTF_8));
            for (DetectionResultDTO result : getAllDetectionResults(reportDto)) {
                String columnInfo = String.format(
                    "%s.%s - Type: %s, Confidence: %.4f, Is PII: %s, Is QI: %s\n",
                    result.getTableName(),
                    result.getColumnName(),
                    result.getPiiType() != null ? result.getPiiType() : "N/A",
                    result.getConfidenceScore(),
                    result.isSensitiveData() ? "Yes" : "No",
                    result.isQuasiIdentifier() ? "Yes" : "No"
                );
                outputStream.write(columnInfo.getBytes(StandardCharsets.UTF_8));
            }
            
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
            
            // Calculate scan duration if it's empty in the DTO
            String scanDuration = reportDto.getScanInfo().getScanDuration();
            if (scanDuration == null || scanDuration.isEmpty()) {
                if (reportDto.getScanInfo().getScanStartTime() != null && reportDto.getScanInfo().getScanEndTime() != null) {
                    try {
                        // Get the Instant objects with proper casting from Object
                        Instant startTime = (Instant) reportDto.getScanInfo().getScanStartTime();
                        Instant endTime = (Instant) reportDto.getScanInfo().getScanEndTime();
                        long durationMillis = Duration.between(startTime, endTime).toMillis();
                        long minutes = durationMillis / (60 * 1000);
                        long seconds = (durationMillis % (60 * 1000)) / 1000;
                        scanDuration = String.format("%d minutes, %d seconds", minutes, seconds);
                        // Update the DTO with calculated duration
                        reportDto.getScanInfo().setScanDuration(scanDuration);
                    } catch (Exception e) {
                        log.warn("Failed to calculate scan duration: {}", e.getMessage());
                        scanDuration = "Unknown";
                    }
                } else {
                    scanDuration = "Unknown";
                }
            }
            
            // Separate columns into different categories
            List<DetectionResultDTO> piiColumns = getPiiFindings(reportDto).stream()
                .filter(dto -> dto.isSensitiveData() && !dto.isQuasiIdentifier())
                .collect(Collectors.toList());
                
            List<DetectionResultDTO> quasiIdentifierColumns = getPiiFindings(reportDto).stream()
                .filter(DetectionResultDTO::isQuasiIdentifier)
                .collect(Collectors.toList());
                
            List<DetectionResultDTO> nonPiiColumns = getAllDetectionResults(reportDto).stream()
                .filter(dto -> !dto.isSensitiveData() && !dto.isQuasiIdentifier())
                .collect(Collectors.toList());
            
            // Count metrics for the summary
            int piiColumnCount = piiColumns.size();
            int quasiIdentifierCount = quasiIdentifierColumns.size();
            int nonPiiColumnCount = nonPiiColumns.size();
            int totalColumnsScanned = reportDto.getScanSummary().getColumnsScanned();
            int notIncludedCount = totalColumnsScanned - (piiColumnCount + quasiIdentifierCount + nonPiiColumnCount);
                
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Write report header using text block
            String header = """
                    PrivSense Scan Report
                    ====================
                    
                    Job ID: %s
                    Database: %s (%s)
                    
                    COLUMN COUNTS:
                    - PII columns: %d
                    - Quasi-Identifier columns: %d
                    - Non-PII columns: %d
                    - Total columns scanned: %d
                    - Columns without detection results: %d
                    
                    Tables scanned: %d
                    Scan duration: %s
                    
                    """.formatted(
                        reportDto.getScanInfo().getScanId(),
                        reportDto.getDatabaseInfo().getName(),
                        reportDto.getDatabaseInfo().getProduct(),
                        piiColumnCount,
                        quasiIdentifierCount,
                        nonPiiColumnCount,
                        totalColumnsScanned,
                        notIncludedCount,
                        reportDto.getScanSummary().getTablesScanned(),
                        reportDto.getScanInfo().getScanDuration()
                    );
            
            outputStream.write(header.getBytes(StandardCharsets.UTF_8));
            
            // ==== SECTION 1: PII COLUMNS ====
            outputStream.write("1. PII COLUMNS\n-------------\n\n".getBytes(StandardCharsets.UTF_8));
            
            if (piiColumns.isEmpty()) {
                outputStream.write("No columns with PII were detected.\n\n".getBytes(StandardCharsets.UTF_8));
            } else {
                // Sort by confidence score, highest first
                piiColumns.sort(Comparator.comparing(DetectionResultDTO::getConfidenceScore).reversed());
                
                for (DetectionResultDTO result : piiColumns) {
                    outputStream.write(String.format(
                        "Column: %s.%s%n",
                        result.getTableName(),
                        result.getColumnName()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Type: %s%n",
                        result.getPiiType()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Confidence Score: %.4f%n",
                        result.getConfidenceScore()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Data Type: %s%n",
                        result.getDataType()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Detection Methods: %s%n%n",
                        String.join(", ", result.getDetectionMethods())
                    ).getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // ==== SECTION 2: QUASI-IDENTIFIER COLUMNS ====
            outputStream.write("2. QUASI-IDENTIFIER COLUMNS\n--------------------------\n\n".getBytes(StandardCharsets.UTF_8));
            
            if (quasiIdentifierColumns.isEmpty()) {
                outputStream.write("No quasi-identifier columns were detected.\n\n".getBytes(StandardCharsets.UTF_8));
            } else {
                // Sort by risk score, highest first
                quasiIdentifierColumns.sort(Comparator.comparing(DetectionResultDTO::getQuasiIdentifierRiskScore).reversed());
                
                for (DetectionResultDTO result : quasiIdentifierColumns) {
                    outputStream.write(String.format(
                        "Column: %s.%s%n",
                        result.getTableName(),
                        result.getColumnName()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Risk Score: %.4f%n",
                        result.getQuasiIdentifierRiskScore()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Data Type: %s%n",
                        result.getDataType()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    if (result.getCorrelatedColumns() != null && !result.getCorrelatedColumns().isEmpty()) {
                        outputStream.write(String.format(
                            "Correlated Columns: %s%n",
                            String.join(", ", result.getCorrelatedColumns())
                        ).getBytes(StandardCharsets.UTF_8));
                    }
                    
                    if (result.getClusteringMethod() != null) {
                        outputStream.write(String.format(
                            "Clustering Method: %s%n",
                            result.getClusteringMethod()
                        ).getBytes(StandardCharsets.UTF_8));
                    }
                    
                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // ==== SECTION 3: NON-PII COLUMNS ====
            outputStream.write("3. NON-PII COLUMNS\n-----------------\n\n".getBytes(StandardCharsets.UTF_8));
            
            if (nonPiiColumns.isEmpty()) {
                outputStream.write("No non-PII columns with detection results were found.\n\n".getBytes(StandardCharsets.UTF_8));
            } else {
                // Sort by table name and then column name
                nonPiiColumns.sort(Comparator
                    .comparing(DetectionResultDTO::getTableName)
                    .thenComparing(DetectionResultDTO::getColumnName));
                
                for (DetectionResultDTO result : nonPiiColumns) {
                    outputStream.write(String.format(
                        "Column: %s.%s%n",
                        result.getTableName(),
                        result.getColumnName()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    outputStream.write(String.format(
                        "Data Type: %s%n",
                        result.getDataType()
                    ).getBytes(StandardCharsets.UTF_8));
                    
                    if (result.getConfidenceScore() > 0) {
                        outputStream.write(String.format(
                            "Confidence Score: %.4f (Below threshold)%n",
                            result.getConfidenceScore()
                        ).getBytes(StandardCharsets.UTF_8));
                        
                        if (result.getPiiType() != null && !result.getPiiType().equals("UNKNOWN")) {
                            outputStream.write(String.format(
                                "Closest PII Type Match: %s%n",
                                result.getPiiType()
                            ).getBytes(StandardCharsets.UTF_8));
                        }
                    }
                    
                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                }
            }
            
            // Write recommendations section
            String recommendations = """
                    Recommendations
                    --------------
                    
                    - Implement data encryption for sensitive columns
                    - Review access controls for PII data
                    - Consider data anonymization techniques for quasi-identifiers
                    - Regularly audit access to PII columns
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