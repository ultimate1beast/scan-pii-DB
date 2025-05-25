package com.privsense.api.repository.jpa;

import com.privsense.core.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing Role entities in the database using JPA.
 * Provides standard CRUD operations and custom finder methods for role-related queries.
 */
@Repository
public interface RoleJpaRepository extends JpaRepository<Role, UUID> {
    /**
     * Find a role by its name
     * @param name The role name to search for
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);
}