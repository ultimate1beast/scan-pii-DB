package com.privsense.api.repository.jpa;

import com.privsense.core.model.DetectionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for DetectionResult entities.
 */
@Repository
public interface DetectionResultJpaRepository extends JpaRepository<DetectionResult, UUID> {
    
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
    @Query("SELECT dr FROM DetectionResult dr WHERE dr.scanMetadata.id = :scanId AND SIZE(dr.candidates) > 0")
    List<DetectionResult> findPiiResultsByScanId(@Param("scanId") UUID scanId);
    
    /**
     * Find detection results by highest confidence PII type.
     *
     * @param piiType The PII type to search for
     * @return List of detection results matching the PII type
     */
    List<DetectionResult> findByHighestConfidencePiiType(String piiType);
}