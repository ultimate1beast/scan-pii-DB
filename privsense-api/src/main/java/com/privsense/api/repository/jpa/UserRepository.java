package com.privsense.api.repository.jpa;

import com.privsense.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for accessing User entities in the database.
 * Provides standard CRUD operations and custom finder methods for user-related queries.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    /**
     * Find a user by their username
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by their email address
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user with the given username exists
     * @param username The username to check
     * @return true if user exists, false otherwise
     */
    boolean existsByUsername(String username);
    
    /**
     * Check if a user with the given email exists
     * @param email The email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);
}