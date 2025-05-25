package com.privsense.api.repository.jpa;

import com.privsense.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for User entities.
 * This interface extends JpaRepository to provide basic CRUD operations.
 */
@Repository
public interface UserJpaRepository extends JpaRepository<User, UUID> {
    
    /**
     * Find a user by username.
     *
     * @param username The username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by email.
     *
     * @param email The email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
}