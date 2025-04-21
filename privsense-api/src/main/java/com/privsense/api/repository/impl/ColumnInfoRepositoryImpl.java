package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.ColumnInfoJpaRepository;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.repository.ColumnInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of ColumnInfoRepository using JPA.
 */
@Repository
public class ColumnInfoRepositoryImpl implements ColumnInfoRepository {

    private final ColumnInfoJpaRepository jpaRepository;

    @Autowired
    public ColumnInfoRepositoryImpl(ColumnInfoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ColumnInfo save(ColumnInfo columnInfo) {
        return jpaRepository.save(columnInfo);
    }

    @Override
    public Iterable<ColumnInfo> saveAll(Collection<ColumnInfo> columnInfos) {
        return jpaRepository.saveAll(columnInfos);
    }

    @Override
    public Optional<ColumnInfo> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<ColumnInfo> findByTableId(UUID tableId) {
        return jpaRepository.findByTableId(tableId);
    }

    @Override
    public Optional<ColumnInfo> findByTableNameAndColumnName(String tableName, String columnName) {
        return jpaRepository.findByTableNameAndColumnName(tableName, columnName);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}