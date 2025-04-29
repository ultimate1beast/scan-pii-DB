package com.privsense.core.service;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing scan jobs.
 */
public interface ScanJobManagementService {

    /**
     * Submits a new scan job.
     *
     * @param requestObj The scan request details (typically a ScanRequest DTO)
     * @return The ID of the created job
     */
    UUID submitScanJob(Object requestObj);

    /**
     * Gets the status of a scan job.
     *
     * @param jobId The job ID
     * @return The job status information (typically a ScanJobResponse DTO)
     */
    Object getJobStatus(UUID jobId);

    /**
     * Cancels an in-progress scan job.
     *
     * @param jobId The job ID
     * @throws IllegalArgumentException if the job doesn't exist
     * @throws IllegalStateException if the job is already completed or failed
     */
    void cancelScan(UUID jobId);

    /**
     * Gets all scan jobs in the system.
     *
     * @return A list of all scan jobs
     */
    List<Object> getAllJobs();
}