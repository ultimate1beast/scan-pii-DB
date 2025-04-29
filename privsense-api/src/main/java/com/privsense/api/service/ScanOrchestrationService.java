package com.privsense.api.service;

import com.privsense.api.dto.ComplianceReportDTO;
import com.privsense.api.dto.ScanJobResponse;
import com.privsense.api.dto.ScanRequest;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service that orchestrates the scan process by delegating to specialized services.
 * This class follows the Facade pattern to provide a simplified interface to the scan subsystem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScanOrchestrationService {

    private final ScanJobManagementService scanJobManagementService;
    private final ScanReportService scanReportService;
    private final ReportExportService reportExportService;

    /**
     * Submits a new scan job.
     *
     * @param scanRequest The scan request parameters
     * @return The ID of the created job
     */
    public UUID submitScanJob(ScanRequest scanRequest) {
        return  scanJobManagementService.submitScanJob(scanRequest);
    }

    /**
     * Gets the status of a scan job.
     *
     * @param jobId The job ID
     * @return The scan job response with status information
     */
    public ScanJobResponse getJobStatus(UUID jobId) {
        return (ScanJobResponse) scanJobManagementService.getJobStatus(jobId);
    }

    /**
     * Cancels an in-progress scan job.
     *
     * @param jobId The job ID
     */
    public void cancelScan(UUID jobId) {
        scanJobManagementService.cancelScan(jobId);
    }

    /**
     * Gets all scan jobs in the system.
     *
     * @return A list of all scan jobs
     */
  
    public List<ScanJobResponse> getAllJobs() {
        List<Object> jobList = scanJobManagementService.getAllJobs();
        return jobList.stream()
                .map(job -> (ScanJobResponse) job)
                .collect(Collectors.toList());
    }

    /**
     * Gets the scan report as a DTO for serialization.
     *
     * @param jobId The job ID
     * @return The compliance report DTO
     */
    public ComplianceReportDTO getScanReportAsDTO(UUID jobId) {
        return (ComplianceReportDTO) scanReportService.getScanReportAsDTO(jobId);
    }

    /**
     * Gets the raw scan report data.
     *
     * @param jobId The job ID
     * @return The compliance report
     */
    public ComplianceReport getRawScanReport(UUID jobId) {
        return scanReportService.getScanReport(jobId);
    }

    /**
     * Exports the scan report as CSV.
     *
     * @param jobId The job ID
     * @return The report as a CSV byte array
     */
    public byte[] exportReportAsCsv(UUID jobId) {
        return reportExportService.exportReportAsCsv(jobId);
    }

    /**
     * Exports the scan report as PDF.
     *
     * @param jobId The job ID
     * @return The report as a PDF byte array
     */
    public byte[] exportReportAsPdf(UUID jobId) {
        return reportExportService.exportReportAsPdf(jobId);
    }

    /**
     * Exports the scan report as text.
     *
     * @param jobId The job ID
     * @return The report as a text byte array
     */
    public byte[] exportReportAsText(UUID jobId) {
        return reportExportService.exportReportAsText(jobId);
    }

    /**
     * Exports the scan report as JSON.
     *
     * @param jobId The job ID
     * @return The report as a JSON byte array
     */
    public byte[] exportReportAsJson(UUID jobId) {
        return reportExportService.exportReportAsJson(jobId);
    }
}