package com.privsense.core.repository;

import com.privsense.core.model.CorrelatedQuasiIdentifierGroup;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing CorrelatedQuasiIdentifierGroup entities.
 * Provides methods to persist and retrieve quasi-identifier groups.
 */
public interface CorrelatedQuasiIdentifierGroupRepository {
    
    /**
     * Save a quasi-identifier group.
     *
     * @param group The quasi-identifier group to save
     * @return The saved group
     */
    CorrelatedQuasiIdentifierGroup save(CorrelatedQuasiIdentifierGroup group);
    
    /**
     * Find a quasi-identifier group by ID.
     *
     * @param id The ID of the group
     * @return Optional containing the group if found
     */
    Optional<CorrelatedQuasiIdentifierGroup> findById(UUID id);
    
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
    List<CorrelatedQuasiIdentifierGroup> findHighRiskGroups(double riskThreshold);
    
    /**
     * Find groups containing a specific column.
     *
     * @param columnId The column ID
     * @return List of quasi-identifier groups containing the column
     */
    List<CorrelatedQuasiIdentifierGroup> findGroupsContainingColumn(UUID columnId);
    
    /**
     * Delete a quasi-identifier group.
     *
     * @param id The ID of the group to delete
     */
    void deleteById(UUID id);
}