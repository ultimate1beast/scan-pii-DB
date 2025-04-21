package com.privsense.api.repository.impl;

import com.privsense.api.repository.jpa.ConnectionJpaRepository;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.repository.ConnectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the ConnectionRepository interface using Spring Data JPA.
 */
@Repository
@Transactional
public class ConnectionRepositoryImpl implements ConnectionRepository {

    private final ConnectionJpaRepository jpaRepository;

    @Autowired
    public ConnectionRepositoryImpl(ConnectionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UUID save(DatabaseConnectionInfo connectionInfo) {
        DatabaseConnectionInfo saved = jpaRepository.save(connectionInfo);
        return saved.getId();
    }

    @Override
    public boolean existsById(UUID connectionId) {
        return jpaRepository.existsById(connectionId);
    }

    @Override
    public Optional<DatabaseConnectionInfo> findById(UUID connectionId) {
        return jpaRepository.findById(connectionId);
    }

    @Override
    public boolean deleteById(UUID connectionId) {
        if (jpaRepository.existsById(connectionId)) {
            jpaRepository.deleteById(connectionId);
            return true;
        }
        return false;
    }

    @Override
    public List<DatabaseConnectionInfo> findAll() {
        return jpaRepository.findAll();
    }
}