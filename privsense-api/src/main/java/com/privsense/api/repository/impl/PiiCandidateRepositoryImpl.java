package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.PiiCandidateJpaRepository;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.repository.PiiCandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of the PiiCandidateRepository interface using Spring Data JPA.
 */
@Repository
public class PiiCandidateRepositoryImpl implements PiiCandidateRepository {

    private final PiiCandidateJpaRepository jpaRepository;

    @Autowired
    public PiiCandidateRepositoryImpl(PiiCandidateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PiiCandidate save(PiiCandidate piiCandidate) {
        return jpaRepository.save(piiCandidate);
    }

    @Override
    public PiiCandidate findById(UUID id) {
        return jpaRepository.findById(id).orElse(null);
    }

    @Override
    public List<PiiCandidate> findByDetectionResultId(UUID resultId) {
        return jpaRepository.findByDetectionResultId(resultId);
    }

    @Override
    public List<PiiCandidate> findByScanId(UUID scanId) {
        return jpaRepository.findByScanId(scanId);
    }

    @Override
    public List<PiiCandidate> findByPiiType(String piiType) {
        return jpaRepository.findByPiiType(piiType);
    }

    @Override
    public List<PiiCandidate> findByDetectionMethod(String detectionMethod) {
        return jpaRepository.findByDetectionMethod(detectionMethod);
    }

    @Override
    public List<PiiCandidate> findHighConfidenceCandidates(double threshold) {
        return jpaRepository.findHighConfidenceCandidates(threshold);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}