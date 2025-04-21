package com.privsense.core.service;

import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.ScanMetadata;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for persisting and retrieving scan results.
 */
public interface ScanPersistenceService {

    /**
     * Create a new scan record
     *
     * @param connectionId The connection ID used for the scan
     * @param databaseName The name of the database being scanned
     * @param databaseProductName The database product name (e.g., PostgreSQL, MySQL)
     * @param databaseProductVersion The database product version
     * @return The created scan metadata entity
     */
    ScanMetadata createScan(UUID connectionId, String databaseName, String databaseProductName, String databaseProductVersion);

    /**
     * Update the status of a scan
     *
     * @param scanId The scan ID
     * @param status The new status
     */
    void updateScanStatus(UUID scanId, ScanMetadata.ScanStatus status);

    /**
     * Save scan results (detection results and PII candidates)
     *
     * @param scanId The scan ID
     * @param results The detection results to save
     */
    void saveScanResults(UUID scanId, List<DetectionResult> results);

    /**
     * Mark a scan as completed
     *
     * @param scanId The scan ID
     */
    void completeScan(UUID scanId);

    /**
     * Mark a scan as failed with an error message
     *
     * @param scanId The scan ID
     * @param errorMessage The error message
     */
    void failScan(UUID scanId, String errorMessage);

    /**
     * Get scan metadata by ID
     *
     * @param scanId The scan ID
     * @return Optional containing the scan metadata if found
     */
    Optional<ScanMetadata> getScanById(UUID scanId);

    /**
     * Get all scans for a specific connection
     *
     * @param connectionId The connection ID
     * @return List of scan metadata entities
     */
    List<ScanMetadata> getScansByConnectionId(UUID connectionId);

    /**
     * Get all scans with a specific status
     *
     * @param status The scan status
     * @return List of scan metadata entities
     */
    List<ScanMetadata> getScansByStatus(ScanMetadata.ScanStatus status);

    /**
     * Get all detection results for a specific scan
     *
     * @param scanId The scan ID
     * @return List of detection result entities
     */
    List<DetectionResult> getDetectionResultsByScanId(UUID scanId);

    /**
     * Get only PII detection results for a specific scan
     *
     * @param scanId The scan ID
     * @return List of detection result entities with PII findings
     */
    List<DetectionResult> getPiiResultsByScanId(UUID scanId);
}