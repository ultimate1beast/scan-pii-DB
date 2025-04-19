package com.privsense.reporter;

import com.privsense.core.exception.ReportGenerationException;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.ScanContext;
import com.privsense.core.service.ComplianceReporter;
import com.privsense.core.service.ReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of the ReportGenerator interface that adapts to the ComplianceReporter.
 * This class serves as an adapter between the ReportGenerator interface required by
 * the ScanOrchestrationService and the existing ComplianceReporter implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JsonReportGeneratorImpl implements ReportGenerator {

    private final ComplianceReporter complianceReporter;

    @Override
    public ComplianceReport generateReport(ScanContext scanContext) {
        // Generate a UUID for this scan if it doesn't have one
        UUID scanId = UUID.randomUUID();
        log.debug("Generating report for scan ID: {}", scanId);
        
        // Configure format options for the report generation
        Map<String, Object> formatOptions = new HashMap<>();
        formatOptions.put("samplingConfig", scanContext.getSamplingConfig());
        formatOptions.put("detectionConfig", scanContext.getDetectionConfig());
        
        // Extract database product name and version if available
        // In a real implementation, these might come from DatabaseMetaData
        String databaseProductName = getDatabaseType(scanContext);
        String databaseProductVersion = "Unknown";
        
        formatOptions.put("databaseProductName", databaseProductName);
        formatOptions.put("databaseProductVersion", databaseProductVersion);
        
        // Configure the reporter with the format options
        complianceReporter.configureReportFormat(formatOptions);
        
        try {
            // Get scan end time (current time if not set)
            LocalDateTime scanEndTime = LocalDateTime.now();
            
            // Convert LocalDateTime to long timestamp
            long scanStartTimestamp = scanContext.getScanStartTime() != null ?
                    scanContext.getScanStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 
                    System.currentTimeMillis();
            
            long scanEndTimestamp = scanEndTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Generate the report using the compliance reporter
            return complianceReporter.generateReport(
                    scanId,
                    scanContext.getDatabaseConnectionInfo(),
                    scanContext.getSchemaInfo(),
                    scanContext.getDetectionResults(),
                    scanStartTimestamp,
                    scanEndTimestamp
            );
        } catch (Exception e) {
            log.error("Failed to generate report for scan ID: {}", scanId, e);
            throw new ReportGenerationException("Failed to generate report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extracts the database type from the connection info
     */
    private String getDatabaseType(ScanContext scanContext) {
        if (scanContext.getDatabaseConnectionInfo() != null && 
            scanContext.getDatabaseConnectionInfo().buildJdbcUrl() != null) {
            
            String jdbcUrl = scanContext.getDatabaseConnectionInfo().buildJdbcUrl().toLowerCase();
            
            if (jdbcUrl.contains("mysql")) {
                return "MySQL";
            } else if (jdbcUrl.contains("postgresql")) {
                return "PostgreSQL";
            } else if (jdbcUrl.contains("sqlserver")) {
                return "Microsoft SQL Server";
            } else if (jdbcUrl.contains("oracle")) {
                return "Oracle";
            }
        }
        
        return "Unknown Database";
    }
}