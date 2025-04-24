package com.privsense.core.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for executing scan jobs.
 */
public interface ScanExecutionService {

    /**
     * Executes a scan job asynchronously.
     *
     * @param jobId The ID of the job
     * @param requestObj The scan request details (typically a ScanRequest DTO)
     * @return A CompletableFuture that completes when the job is finished
     */
    CompletableFuture<Void> executeScanAsync(UUID jobId, Object requestObj);
}