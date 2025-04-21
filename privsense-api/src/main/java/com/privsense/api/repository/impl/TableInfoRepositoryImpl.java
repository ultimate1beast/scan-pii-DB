package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.TableInfoJpaRepository;
import com.privsense.core.model.TableInfo;
import com.privsense.core.repository.TableInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of TableInfoRepository using JPA.
 */
@Repository
public class TableInfoRepositoryImpl implements TableInfoRepository {

    private final TableInfoJpaRepository jpaRepository;

    @Autowired
    public TableInfoRepositoryImpl(TableInfoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TableInfo save(TableInfo tableInfo) {
        return jpaRepository.save(tableInfo);
    }

    @Override
    public Iterable<TableInfo> saveAll(Collection<TableInfo> tableInfos) {
        return jpaRepository.saveAll(tableInfos);
    }

    @Override
    public Optional<TableInfo> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<TableInfo> findBySchemaId(UUID schemaId) {
        return jpaRepository.findBySchemaId(schemaId);
    }

    @Override
    public Optional<TableInfo> findBySchemaIdAndTableName(UUID schemaId, String tableName) {
        return jpaRepository.findBySchemaIdAndTableName(schemaId, tableName);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}