package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.ComplianceReportJpaRepository;
import com.privsense.core.model.ComplianceReport;
import com.privsense.core.repository.ComplianceReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}