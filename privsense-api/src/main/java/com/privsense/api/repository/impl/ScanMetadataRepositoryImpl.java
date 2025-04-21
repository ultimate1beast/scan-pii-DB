package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.ScanMetadataJpaRepository;
import com.privsense.core.model.ScanMetadata;
import com.privsense.core.repository.ScanMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the ScanMetadataRepository interface using Spring Data JPA.
 */
@Repository
public class ScanMetadataRepositoryImpl implements ScanMetadataRepository {

    private final ScanMetadataJpaRepository jpaRepository;

    @Autowired
    public ScanMetadataRepositoryImpl(ScanMetadataJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ScanMetadata save(ScanMetadata scanMetadata) {
        return jpaRepository.save(scanMetadata);
    }

    @Override
    public Optional<ScanMetadata> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ScanMetadata> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ScanMetadata> findByConnectionId(UUID connectionId) {
        return jpaRepository.findByConnectionId(connectionId);
    }

    @Override
    public List<ScanMetadata> findByStatus(ScanMetadata.ScanStatus status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<ScanMetadata> findRecentScans(Instant startTime) {
        return jpaRepository.findRecentScans(startTime);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}