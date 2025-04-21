package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.RelationshipInfoJpaRepository;
import com.privsense.core.model.RelationshipInfo;
import com.privsense.core.repository.RelationshipInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of RelationshipInfoRepository using JPA.
 */
@Repository
public class RelationshipInfoRepositoryImpl implements RelationshipInfoRepository {

    private final RelationshipInfoJpaRepository jpaRepository;

    @Autowired
    public RelationshipInfoRepositoryImpl(RelationshipInfoJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RelationshipInfo save(RelationshipInfo relationshipInfo) {
        return jpaRepository.save(relationshipInfo);
    }

    @Override
    public Iterable<RelationshipInfo> saveAll(Collection<RelationshipInfo> relationshipInfos) {
        return jpaRepository.saveAll(relationshipInfos);
    }

    @Override
    public Optional<RelationshipInfo> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<RelationshipInfo> findBySourceTableId(UUID sourceTableId) {
        return jpaRepository.findBySourceTableId(sourceTableId);
    }

    @Override
    public List<RelationshipInfo> findByTargetTableId(UUID targetTableId) {
        return jpaRepository.findByTargetTableId(targetTableId);
    }

    @Override
    public Optional<RelationshipInfo> findByConstraintName(String constraintName) {
        return jpaRepository.findByConstraintName(constraintName);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}