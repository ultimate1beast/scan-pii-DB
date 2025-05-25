package com.privsense.core.service;

import com.privsense.core.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for managing user accounts.
 */
public interface UserService {
    
    /**
     * Registers a new user in the system.
     *
     * @param username the username
     * @param email the email address
     * @param password the plain text password (will be encrypted)
     * @param firstName the first name
     * @param lastName the last name
     * @return the created user
     */
    User registerUser(String username, String email, String password, String firstName, String lastName);
    
    /**
     * Finds a user by their ID.
     *
     * @param id the user ID
     * @return an Optional containing the user if found
     */
    Optional<User> findById(UUID id);
    
    /**
     * Finds a user by their username.
     *
     * @param username the username
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Updates a user's profile information.
     *
     * @param id the user ID
     * @param firstName the new first name
     * @param lastName the new last name
     * @param email the new email address
     * @return the updated user
     */
    User updateProfile(UUID id, String firstName, String lastName, String email);
    
    /**
     * Changes a user's password.
     *
     * @param id the user ID
     * @param currentPassword the current password for verification
     * @param newPassword the new password
     * @return true if the password was changed successfully, false otherwise
     */
    boolean changePassword(UUID id, String currentPassword, String newPassword);
    
    /**
     * Updates a user's roles.
     *
     * @param id the user ID
     * @param roleNames the set of role names to assign
     * @return the updated user
     */
    User updateRoles(UUID id, Set<String> roleNames);
    
    /**
     * Gets all users in the system.
     *
     * @return a list of all users
     */
    List<User> getAllUsers();
    
    /**
     * Updates the last login timestamp for a user.
     *
     * @param id the user ID
     */
    void updateLastLogin(UUID id);
    
    /**
     * Saves a user entity.
     * 
     * @param user the user to save
     * @return the saved user
     */
    User save(User user);
}