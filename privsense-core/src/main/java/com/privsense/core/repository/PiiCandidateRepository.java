package com.privsense.core.repository;

import com.privsense.core.model.PiiCandidate;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing PiiCandidate entities.
 */
public interface PiiCandidateRepository {

    /**
     * Save a PII candidate.
     *
     * @param piiCandidate The PII candidate to save
     * @return The saved entity
     */
    PiiCandidate save(PiiCandidate piiCandidate);

    /**
     * Find a PII candidate by its ID.
     *
     * @param id The ID of the PII candidate
     * @return The PII candidate if found, or null otherwise
     */
    PiiCandidate findById(UUID id);

    /**
     * Find all PII candidates for a specific detection result.
     *
     * @param resultId The detection result ID
     * @return List of PII candidate entities
     */
    List<PiiCandidate> findByDetectionResultId(UUID resultId);
    
    /**
     * Find all PII candidates for a specific scan.
     *
     * @param scanId The scan ID
     * @return List of PII candidate entities
     */
    List<PiiCandidate> findByScanId(UUID scanId);
    
    /**
     * Find PII candidates by PII type.
     *
     * @param piiType The PII type to search for
     * @return List of PII candidate entities
     */
    List<PiiCandidate> findByPiiType(String piiType);
    
    /**
     * Find PII candidates by detection method.
     *
     * @param detectionMethod The detection method to search for
     * @return List of PII candidate entities
     */
    List<PiiCandidate> findByDetectionMethod(String detectionMethod);
    
    /**
     * Find high-confidence PII candidates (above a threshold).
     *
     * @param threshold The confidence threshold
     * @return List of PII candidate entities
     */
    List<PiiCandidate> findHighConfidenceCandidates(double threshold);
    
    /**
     * Delete a PII candidate.
     *
     * @param id The ID of the PII candidate to delete
     */
    void deleteById(UUID id);
}