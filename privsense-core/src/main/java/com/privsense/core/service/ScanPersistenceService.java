package com.privsense.core.service;

import com.privsense.core.model.ComplianceReport;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.model.ScanMetadata;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for persisting and retrieving scan data.
 * Follows the Repository pattern to provide a clean separation between the domain model layer
 * and the data mapping layer.
 */
public interface ScanPersistenceService {
    
    /**
     * Creates a new scan record.
     *
     * @param connectionId The ID of the database connection
     * @param databaseName The name of the database being scanned
     * @param databaseProductName The database product name
     * @param databaseProductVersion The database product version
     * @return The created ScanMetadata
     */
    ScanMetadata createScan(UUID connectionId, String databaseName, String databaseProductName, String databaseProductVersion);
    
    /**
     * Updates the status of a scan.
     *
     * @param scanId The ID of the scan
     * @param status The new status
     */
    void updateScanStatus(UUID scanId, ScanMetadata.ScanStatus status);
    
    /**
     * Saves scan results to the database.
     *
     * @param scanId The ID of the scan
     * @param results The detection results to save
     */
    void saveScanResults(UUID scanId, List<DetectionResult> results);
    
    /**
     * Marks a scan as completed.
     *
     * @param scanId The ID of the scan
     */
    void completeScan(UUID scanId);
    
    /**
     * Marks a scan as failed with an error message.
     *
     * @param scanId The ID of the scan
     * @param errorMessage The error message
     */
    void failScan(UUID scanId, String errorMessage);
    
    /**
     * Gets a scan by its ID.
     *
     * @param scanId The ID of the scan
     * @return An Optional containing the scan if found
     */
    Optional<ScanMetadata> getScanById(UUID scanId);
    
    /**
     * Gets all scans for a connection.
     *
     * @param connectionId The ID of the connection
     * @return A list of scans for the connection
     */
    List<ScanMetadata> getScansByConnectionId(UUID connectionId);
    
    /**
     * Gets all scans with a specific status.
     *
     * @param status The status to filter by
     * @return A list of scans with the given status
     */
    List<ScanMetadata> getScansByStatus(ScanMetadata.ScanStatus status);
    
    /**
     * Gets all scans with a specific status for a specific connection.
     *
     * @param status The status to filter by
     * @param connectionId The connection ID to filter by
     * @return A list of scans matching both filters
     */
    List<ScanMetadata> getScansByStatusAndConnectionId(ScanMetadata.ScanStatus status, UUID connectionId);
    
    /**
     * Gets a paginated list of scans.
     *
     * @param page The page number (0-based)
     * @param size The page size
     * @return A list of scans for the requested page
     */
    List<ScanMetadata> getPagedScans(int page, int size);
    
    /**
     * Gets the total count of all scans.
     *
     * @return The total number of scan records
     */
    long countAllScans();
    
    /**
     * Gets all detection results for a scan.
     *
     * @param scanId The ID of the scan
     * @return A list of detection results
     */
    List<DetectionResult> getDetectionResultsByScanId(UUID scanId);
    
    /**
     * Gets only PII detection results for a scan.
     *
     * @param scanId The ID of the scan
     * @return A list of PII detection results
     */
    List<DetectionResult> getPiiResultsByScanId(UUID scanId);
    
    /**
     * Saves or updates a scan metadata record.
     *
     * @param scanMetadata The scan metadata to save
     * @return The saved scan metadata
     */
    ScanMetadata save(ScanMetadata scanMetadata);
    
    /**
     * Gets all scans in the system.
     *
     * @return A list of all scans
     */
    List<ScanMetadata> getAllScans();
    
    /**
     * Saves a compliance report for a scan.
     *
     * @param scanId The ID of the scan
     * @param report The compliance report to save
     */
    void saveReport(UUID scanId, ComplianceReport report);
    
    /**
     * Gets scans within a specified time range.
     *
     * @param startTime The start of the time range
     * @param endTime The end of the time range
     * @return List of scans with start times within the range
     */
    List<ScanMetadata> getScansByTimeRange(Instant startTime, Instant endTime);
    
    /**
     * Gets the most recent scans, ordered by start time.
     *
     * @param limit The maximum number of scans to return
     * @return List of the most recent scans
     */
    List<ScanMetadata> getRecentScans(int limit);
}