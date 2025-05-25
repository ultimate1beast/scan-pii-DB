package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.ScanTemplateJpaRepository;
import com.privsense.core.model.ScanTemplate;
import com.privsense.core.repository.ScanTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the ScanTemplateRepository using JPA.
 */
@Repository
public class ScanTemplateRepositoryImpl implements ScanTemplateRepository {

    private final ScanTemplateJpaRepository jpaRepository;

    @Autowired
    public ScanTemplateRepositoryImpl(ScanTemplateJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ScanTemplate save(ScanTemplate template) {
        return jpaRepository.save(template);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public Optional<ScanTemplate> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ScanTemplate> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<ScanTemplate> findByConnectionId(UUID connectionId) {
        return jpaRepository.findByConnectionId(connectionId);
    }

    @Override
    public boolean deleteById(UUID id) {
        if (jpaRepository.existsById(id)) {
            jpaRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }
}