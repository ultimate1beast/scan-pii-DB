package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.ComplianceReportJpaRepository;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.repository.ComplianceReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ComplianceReportRepository using JPA.
 */
@Repository
public class ComplianceReportRepositoryImpl implements ComplianceReportRepository {

    private final ComplianceReportJpaRepository jpaRepository;

    @Autowired
    public ComplianceReportRepositoryImpl(ComplianceReportJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ComplianceReport save(ComplianceReport report) {
        return jpaRepository.save(report);
    }

    @Override
    public Optional<ComplianceReport> findByScanId(UUID scanId) {
        return jpaRepository.findByScanId(scanId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<ComplianceReport> findByScanIdWithDetectionResults(UUID scanId) {
        // Use the dedicated repository method with JOIN FETCH to eagerly load the collection
        try {
            return jpaRepository.findComplianceReportWithDetectionResults(scanId);
        } catch (Exception e) {
            // Fallback to standard find and initialize the collection within the transaction
            Optional<ComplianceReport> reportOpt = jpaRepository.findByScanId(scanId);
            
            // Initialize the detection results collection if the report exists
            reportOpt.ifPresent(report -> {
                if (report.getDetectionResults() != null) {
                    // Force initialization by accessing the collection
                    report.getDetectionResults().size();
                }
            });
            
            return reportOpt;
        }
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}