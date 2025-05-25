package com.privsense.core.repository;

import com.privsense.core.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for performing database operations on Role entities.
 */
public interface RoleRepository extends JpaRepository<Role, UUID> {
    
    /**
     * Find a role by name.
     *
     * @param name the role name to search for
     * @return an Optional containing the role if found
     */
    Optional<Role> findByName(String name);
    
    /**
     * Find all roles with names matching the provided set of names.
     *
     * @param names the role names to search for
     * @return a set of roles that match the provided names
     */
    Set<Role> findByNameIn(Set<String> names);
    
    /**
     * Check if a role exists with the specified name.
     *
     * @param name the role name to check
     * @return true if the role exists, false otherwise
     */
    boolean existsByName(String name);
}