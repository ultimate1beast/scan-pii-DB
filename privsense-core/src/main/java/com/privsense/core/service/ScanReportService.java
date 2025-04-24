package com.privsense.core.service;

import com.privsense.core.model.ComplianceReport;

import java.util.UUID;

/**
 * Service interface for retrieving and processing scan reports.
 */
public interface ScanReportService {

    /**
     * Gets the scan report for a job.
     *
     * @param jobId The job ID
     * @return The compliance report
     * @throws IllegalArgumentException if the job doesn't exist
     * @throws IllegalStateException if the report isn't ready
     */
    ComplianceReport getScanReport(UUID jobId);

    /**
     * Gets the scan report as a serializable object.
     * Note: The implementation should return an appropriate DTO object
     * that can be serialized to JSON/XML.
     *
     * @param jobId The job ID
     * @return An object representing the compliance report ready for serialization
     * @throws IllegalArgumentException if the job doesn't exist
     * @throws IllegalStateException if the report isn't ready
     */
    Object getScanReportAsDTO(UUID jobId);
}