package com.privsense.core.repository;

import com.privsense.core.model.DetectionResult;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing detection results.
 */
public interface DetectionResultRepository {
    
    /**
     * Save a detection result.
     *
     * @param detectionResult The detection result entity to save
     * @return The saved entity
     */
    DetectionResult save(DetectionResult detectionResult);
    
    /**
     * Find a detection result by its ID.
     *
     * @param id The ID of the detection result
     * @return The detection result if found, or null otherwise
     */
    DetectionResult findById(UUID id);
    
    /**
     * Find all detection results for a specific scan.
     *
     * @param scanId The scan ID
     * @return List of detection result entities
     */
    List<DetectionResult> findByScanMetadataId(UUID scanId);
    
    /**
     * Find all detection results with PII findings for a specific scan.
     *
     * @param scanId The scan ID
     * @return List of detection results with PII findings
     */
    List<DetectionResult> findPiiResultsByScanId(UUID scanId);
    
    /**
     * Find detection results by specific PII type.
     *
     * @param piiType The PII type to search for
     * @return List of detection results matching the PII type
     */
    List<DetectionResult> findByHighestConfidencePiiType(String piiType);
    
    /**
     * Delete a detection result.
     *
     * @param id The ID of the detection result to delete
     */
    void deleteById(UUID id);
}