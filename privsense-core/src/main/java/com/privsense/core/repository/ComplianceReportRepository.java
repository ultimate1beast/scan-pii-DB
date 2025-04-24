package com.privsense.core.repository;

import com.privsense.core.model.ComplianceReport;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing ComplianceReport entities.
 */
public interface ComplianceReportRepository {
    
    /**
     * Save a compliance report.
     *
     * @param report The compliance report to save
     * @return The saved report
     */
    ComplianceReport save(ComplianceReport report);
    
    /**
     * Find a compliance report by scan ID.
     *
     * @param scanId The scan ID
     * @return Optional containing the compliance report if found
     */
    Optional<ComplianceReport> findByScanId(UUID scanId);
    
    /**
     * Delete a compliance report by ID.
     *
     * @param id The ID of the report to delete
     */
    void deleteById(UUID id);
}