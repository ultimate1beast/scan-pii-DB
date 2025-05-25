package com.privsense.api.repository.jpa;

import com.privsense.core.model.CorrelatedQuasiIdentifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository interface for CorrelatedQuasiIdentifierGroup entities.
 */
@Repository
public interface CorrelatedQuasiIdentifierGroupJpaRepository extends JpaRepository<CorrelatedQuasiIdentifierGroup, UUID> {
    
    /**
     * Find all quasi-identifier groups for a specific scan.
     *
     * @param scanId The scan ID
     * @return List of quasi-identifier groups
     */
    List<CorrelatedQuasiIdentifierGroup> findByScanMetadataId(UUID scanId);
    
    /**
     * Find high-risk quasi-identifier groups (above a threshold).
     *
     * @param riskThreshold The risk score threshold
     * @return List of high-risk quasi-identifier groups
     */
    @Query("SELECT qig FROM CorrelatedQuasiIdentifierGroup qig WHERE qig.reIdentificationRiskScore >= :threshold")
    List<CorrelatedQuasiIdentifierGroup> findByRiskScoreGreaterThanEqual(@Param("threshold") double riskThreshold);
    
    /**
     * Find groups containing a specific column.
     *
     * @param columnId The column ID
     * @return List of quasi-identifier groups containing the column
     */
    @Query("SELECT qig FROM CorrelatedQuasiIdentifierGroup qig JOIN qig.columns qicm WHERE qicm.column.id = :columnId")
    List<CorrelatedQuasiIdentifierGroup> findByColumnId(@Param("columnId") UUID columnId);
}