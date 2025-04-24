package com.privsense.api.repository.jpa;

import com.privsense.core.model.ComplianceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA Repository for ComplianceReport entities.
 */
@Repository
public interface ComplianceReportJpaRepository extends JpaRepository<ComplianceReport, UUID> {
    
    /**
     * Find a compliance report by scan ID.
     *
     * @param scanId The scan ID
     * @return Optional containing the compliance report if found
     */
    Optional<ComplianceReport> findByScanId(UUID scanId);
}