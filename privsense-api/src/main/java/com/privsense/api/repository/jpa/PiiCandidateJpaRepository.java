package com.privsense.api.repository.jpa;

import com.privsense.core.model.PiiCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for PiiCandidate entities.
 */
@Repository
public interface PiiCandidateJpaRepository extends JpaRepository<PiiCandidate, UUID> {
    
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
    @Query("SELECT pc FROM PiiCandidate pc WHERE pc.detectionResult.scanMetadata.id = :scanId")
    List<PiiCandidate> findByScanId(@Param("scanId") UUID scanId);
    
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
    @Query("SELECT pc FROM PiiCandidate pc WHERE pc.confidenceScore >= :threshold")
    List<PiiCandidate> findHighConfidenceCandidates(@Param("threshold") double threshold);
}