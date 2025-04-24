package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.SchemaInfoJpaRepository;
import com.privsense.core.model.SchemaInfo;
import com.privsense.core.repository.SchemaInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of SchemaInfoRepository using JPA.
 */
@Repository
public class SchemaInfoRepositoryImpl implements SchemaInfoRepository {

    private final SchemaInfoJpaRepository jpaRepository;

    @Autowired
    public SchemaInfoRepositoryImpl(SchemaInfoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SchemaInfo save(SchemaInfo schemaInfo) {
        return jpaRepository.save(schemaInfo);
    }

    @Override
    public Iterable<SchemaInfo> saveAll(Collection<SchemaInfo> schemaInfos) {
        return jpaRepository.saveAll(schemaInfos);
    }

    @Override
    public Optional<SchemaInfo> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SchemaInfo> findByScanId(UUID scanId) {
        return jpaRepository.findByScanId(scanId);
    }

    @Override
    public Optional<SchemaInfo> findBySchemaName(String schemaName) {
        return jpaRepository.findBySchemaName(schemaName);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }
}