package com.privsense.api.repository;

import com.privsense.api.repository.jpa.ConnectionJpaRepository;
import com.privsense.core.model.DatabaseConnectionInfo;
import com.privsense.core.repository.ConnectionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter class that implements the core ConnectionRepository interface 
 * while delegating to Spring Data JPA's ConnectionJpaRepository.
 * This allows components depending on the ConnectionRepository interface
 * to work with Spring Boot's auto-configuration.
 */
public class JpaConnectionRepository implements ConnectionRepository {

    private final ConnectionJpaRepository jpaRepository;

    public JpaConnectionRepository(ConnectionJpaRepository jpaRepository) {
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