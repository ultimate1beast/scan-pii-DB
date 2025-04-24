package com.privsense.api.service.impl;

import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.repository.ComplianceReportRepository;
import com.privsense.core.service.ScanJobManagementService;
import com.privsense.core.service.ScanPersistenceService;
import com.privsense.core.service.ScanReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for retrieving and processing scan reports.
 */
@Service
@Slf4j
public class ScanReportServiceImpl implements ScanReportService {

    private final ScanPersistenceService scanPersistenceService;
    private final ScanJobManagementService scanJobManagementService;
    private final ComplianceReportRepository complianceReportRepository;

    @Autowired
    public ScanReportServiceImpl(
            ScanPersistenceService scanPersistenceService,
            @Lazy ScanJobManagementService scanJobManagementService,
            ComplianceReportRepository complianceReportRepository) {
        this.scanPersistenceService = scanPersistenceService;
        this.scanJobManagementService = scanJobManagementService;
        this.complianceReportRepository = complianceReportRepository;
    }

    @Override
    public ComplianceReport getScanReport(UUID jobId) {
        // First check if the job is completed
        Object jobResponse = scanJobManagementService.getJobStatus(jobId);
        if (!isJobCompleted(jobResponse)) {
            throw new IllegalStateException("Scan is not complete. Current status: " + 
                getStatusFromJobResponse(jobResponse));
        }

        // Try to find the report in the database
        Optional<ComplianceReport> reportOpt = complianceReportRepository.findByScanId(jobId);
        
        if (reportOpt.isPresent()) {
            return reportOpt.get();
        } else {
            throw new IllegalStateException("Report not found for completed scan: " + jobId);
        }
    }

    @Override
    public ComplianceReportDTO getScanReportAsDTO(UUID jobId) {
        ComplianceReport report = getScanReport(jobId);
        
        // Get scan metadata to include additional information
        if (!scanPersistenceService.getScanById(jobId).isPresent()) {
            throw new IllegalArgumentException("Scan not found: " + jobId);
        }
        
        // Convert the report to DTO with additional information
        ComplianceReportDTO dto = new ComplianceReportDTO();
        dto.setReportId(report.getReportId());
        dto.setScanId(report.getScanId());
        dto.setDatabaseHost(report.getDatabaseHost());
        dto.setDatabaseName(report.getDatabaseName());
        dto.setDatabaseProductName(report.getDatabaseProductName());
        dto.setDatabaseProductVersion(report.getDatabaseProductVersion());
        dto.setTotalTablesScanned(report.getTotalTablesScanned());
        dto.setTotalColumnsScanned(report.getTotalColumnsScanned());
        dto.setTotalPiiColumnsFound(report.getTotalPiiColumnsFound());
        dto.setScanStartTime(report.getScanStartTime());
        dto.setScanEndTime(report.getScanEndTime());
        dto.setScanDuration(report.getScanDuration());
        dto.setSamplingConfig(report.getSamplingConfig());
        dto.setDetectionConfig(report.getDetectionConfig());
        
        // Get detailed PII findings
        List<DetectionResult> piiResults = scanPersistenceService.getPiiResultsByScanId(jobId);
        
        // Convert to DTOs using Stream.toList() (Java 16+)
        List<ComplianceReportDTO.PiiColumnDTO> findings = piiResults.stream()
                .map(this::mapToFindingDTO)
                .toList();
                
        dto.setPiiFindings(findings);
        
        return dto;
    }

    /**
     * Helper method to convert a detection result to a PII finding DTO
     */
    private ComplianceReportDTO.PiiColumnDTO mapToFindingDTO(DetectionResult result) {
        ComplianceReportDTO.PiiColumnDTO dto = new ComplianceReportDTO.PiiColumnDTO();
        dto.setTableName(result.getColumnInfo().getTable().getTableName());
        dto.setColumnName(result.getColumnInfo().getColumnName());
        dto.setPiiType(result.getHighestConfidencePiiType());
        dto.setConfidenceScore(result.getHighestConfidenceScore());
        dto.setDetectionMethods(result.getDetectionMethods());
        
        // Only include schema name if it's not null
        if (result.getColumnInfo().getTable().getSchema() != null) {
            // Can add schema name if needed
        }
        
        return dto;
    }

    /**
     * Helper method to check if a job is completed
     */
    private boolean isJobCompleted(Object jobResponse) {
        // Check if it's a ScanJobResponse
        if (jobResponse instanceof ScanJobResponse) {
            // Cast to ScanJobResponse and use isCompleted() method
            return ((ScanJobResponse)jobResponse).isCompleted();
        } else if (jobResponse.getClass().getName().endsWith("ScanJobResponse")) {
            try {
                // Use reflection to access the "isCompleted" method
                return (Boolean)jobResponse.getClass().getMethod("isCompleted").invoke(jobResponse);
            } catch (Exception e) {
                log.error("Failed to check job completion status", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Helper method to get status string from job response
     */
    private String getStatusFromJobResponse(Object jobResponse) {
        // Check if it's a ScanJobResponse
        if (jobResponse instanceof ScanJobResponse) {
            // Cast to ScanJobResponse and use getStatus() method
            return ((ScanJobResponse)jobResponse).getStatus();
        } else if (jobResponse.getClass().getName().endsWith("ScanJobResponse")) {
            try {
                return (String) jobResponse.getClass().getMethod("getStatus").invoke(jobResponse);
            } catch (Exception e) {
                log.error("Failed to get job status", e);
                return "UNKNOWN";
            }
        }
        return "UNKNOWN";
    }
}