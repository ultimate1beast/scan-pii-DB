package com.privsense.api.repository.jpa;

import com.privsense.core.model.DatabaseConnectionInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for DatabaseConnectionInfo entities.
 */
@Repository
public interface ConnectionJpaRepository extends JpaRepository<DatabaseConnectionInfo, UUID> {
    // Spring Data JPA provides all the basic CRUD operations
}