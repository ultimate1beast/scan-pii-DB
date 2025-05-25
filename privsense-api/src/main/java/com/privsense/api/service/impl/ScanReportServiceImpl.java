package com.privsense.api.service.impl;

import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.mapper.EntityMapper;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.repository.ComplianceReportRepository;
import com.privsense.core.service.ScanJobManagementService;
import com.privsense.core.service.ScanPersistenceService;
import com.privsense.core.service.ScanReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    private final EntityMapper entityMapper;

    @Autowired
    public ScanReportServiceImpl(
            ScanPersistenceService scanPersistenceService,
            @Lazy ScanJobManagementService scanJobManagementService,
            ComplianceReportRepository complianceReportRepository,
            EntityMapper entityMapper) {
        this.scanPersistenceService = scanPersistenceService;
        this.scanJobManagementService = scanJobManagementService;
        this.complianceReportRepository = complianceReportRepository;
        this.entityMapper = entityMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceReport getScanReport(UUID jobId) {
        // First check if the job is completed
        Object jobResponse = scanJobManagementService.getJobStatus(jobId);
        if (!isJobCompleted(jobResponse)) {
            throw new IllegalStateException("Scan is not complete. Current status: " + 
                getStatusFromJobResponse(jobResponse));
        }

        // Try to find the report with eagerly loaded detection results
        Optional<ComplianceReport> reportOpt = complianceReportRepository.findByScanIdWithDetectionResults(jobId);
        
        if (reportOpt.isPresent()) {
            ComplianceReport report = reportOpt.get();
            // Initialize the transient scan duration field
            report.initializeScanDuration();
            return report;
        } else {
            throw new IllegalStateException("Report not found for completed scan: " + jobId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ComplianceReportDTO getScanReportAsDTO(UUID jobId) {
        // Get the report entity from the database
        ComplianceReport report = getScanReport(jobId);
        
        // Verify the scan exists
        if (!scanPersistenceService.getScanById(jobId).isPresent()) {
            throw new IllegalArgumentException("Scan not found: " + jobId);
        }
        
        // Use the EntityMapper to convert the report entity to DTO
        // This approach avoids circular references in the object graph
        ComplianceReportDTO dto = entityMapper.toDto(report);
        
        // Explicitly mark the report as successful so that the success field is true in the JSON response
        dto.addMeta("status", "SUCCESS");
        
        // Also set success flag for all nested detection results in the table findings
        if (dto.getTableFindings() != null) {
            dto.getTableFindings().values().stream()
                .filter(tableFinding -> tableFinding.getColumns() != null)
                .forEach(tableFinding -> 
                    tableFinding.getColumns().forEach(column -> 
                        column.addMeta("status", "SUCCESS")
                    )
                );
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