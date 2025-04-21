package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.DetectionResultJpaRepository;
import com.privsense.core.model.DetectionResult;
import com.privsense.core.repository.DetectionResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of the DetectionResultRepository interface using Spring Data JPA.
 */
@Repository
public class DetectionResultRepositoryImpl implements DetectionResultRepository {

    private final DetectionResultJpaRepository jpaRepository;

    @Autowired
    public DetectionResultRepositoryImpl(DetectionResultJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DetectionResult save(DetectionResult detectionResult) {
        return jpaRepository.save(detectionResult);
    }

    @Override
    public DetectionResult findById(UUID id) {
        return jpaRepository.findById(id).orElse(null);
    }

    @Override
    public List<DetectionResult> findByScanMetadataId(UUID scanId) {
        return jpaRepository.findByScanMetadataId(scanId);
    }

    @Override
    public List<DetectionResult> findPiiResultsByScanId(UUID scanId) {
        return jpaRepository.findPiiResultsByScanId(scanId);
    }

    @Override
    public List<DetectionResult> findByHighestConfidencePiiType(String piiType) {
        return jpaRepository.findByHighestConfidencePiiType(piiType);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}