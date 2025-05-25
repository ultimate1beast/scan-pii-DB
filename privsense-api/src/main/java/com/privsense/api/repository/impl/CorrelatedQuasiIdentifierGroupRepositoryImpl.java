package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.CorrelatedQuasiIdentifierGroupJpaRepository;
import com.privsense.core.model.CorrelatedQuasiIdentifierGroup;
import com.privsense.core.repository.CorrelatedQuasiIdentifierGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the CorrelatedQuasiIdentifierGroupRepository interface using Spring Data JPA.
 */
@Repository
public class CorrelatedQuasiIdentifierGroupRepositoryImpl implements CorrelatedQuasiIdentifierGroupRepository {

    private final CorrelatedQuasiIdentifierGroupJpaRepository jpaRepository;

    @Autowired
    public CorrelatedQuasiIdentifierGroupRepositoryImpl(CorrelatedQuasiIdentifierGroupJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CorrelatedQuasiIdentifierGroup save(CorrelatedQuasiIdentifierGroup group) {
        return jpaRepository.save(group);
    }

    @Override
    public Optional<CorrelatedQuasiIdentifierGroup> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<CorrelatedQuasiIdentifierGroup> findByScanMetadataId(UUID scanId) {
        return jpaRepository.findByScanMetadataId(scanId);
    }

    @Override
    public List<CorrelatedQuasiIdentifierGroup> findHighRiskGroups(double riskThreshold) {
        return jpaRepository.findByRiskScoreGreaterThanEqual(riskThreshold);
    }

    @Override
    public List<CorrelatedQuasiIdentifierGroup> findGroupsContainingColumn(UUID columnId) {
        return jpaRepository.findByColumnId(columnId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}