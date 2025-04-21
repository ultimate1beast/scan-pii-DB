package com.privsense.core.repository;

import com.privsense.core.model.ScanMetadata;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing ScanMetadata entities.
 */
public interface ScanMetadataRepository {
    
    /**
     * Save a scan metadata entity.
     *
     * @param scanMetadata The scan metadata to save
     * @return The saved entity
     */
    ScanMetadata save(ScanMetadata scanMetadata);
    
    /**
     * Find a scan metadata by its ID.
     *
     * @param id The ID of the scan metadata
     * @return Optional containing the scan metadata if found
     */
    Optional<ScanMetadata> findById(UUID id);
    
    /**
     * Find all scan metadata entities.
     *
     * @return List of all scan metadata entities
     */
    List<ScanMetadata> findAll();
    
    /**
     * Find all scans for a specific connection.
     *
     * @param connectionId The connection ID
     * @return List of scan metadata entities
     */
    List<ScanMetadata> findByConnectionId(UUID connectionId);
    
    /**
     * Find all scans with a specific status.
     *
     * @param status The scan status
     * @return List of scan metadata entities
     */
    List<ScanMetadata> findByStatus(ScanMetadata.ScanStatus status);
    
    /**
     * Find recent scans performed after a specific time.
     *
     * @param startTime The start time to search from
     * @return List of scan metadata entities
     */
    List<ScanMetadata> findRecentScans(Instant startTime);
    
    /**
     * Delete a scan metadata.
     *
     * @param id The ID of the scan metadata to delete
     */
    void deleteById(UUID id);
}