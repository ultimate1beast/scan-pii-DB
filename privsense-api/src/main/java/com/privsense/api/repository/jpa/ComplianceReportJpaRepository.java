package com.privsense.api.repository.jpa;

import com.privsense.core.model.ComplianceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * Find a compliance report by scan ID with detection results eagerly loaded.
     * This uses a JOIN FETCH to prevent LazyInitializationException when the session is closed.
     *
     * @param scanId The scan ID
     * @return Optional containing the compliance report with initialized detection results if found
     */
    @Query("SELECT cr FROM ComplianceReport cr LEFT JOIN FETCH cr.detectionResults WHERE cr.scanId = :scanId")
    Optional<ComplianceReport> findComplianceReportWithDetectionResults(@Param("scanId") UUID scanId);
}