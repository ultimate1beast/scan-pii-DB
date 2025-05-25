package com.privsense.api.service.auth;

import com.privsense.api.dto.auth.RegisterRequest;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.mapper.UserMapper;
import com.privsense.api.repository.jpa.RoleJpaRepository;
import com.privsense.api.repository.jpa.UserRepository;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing user accounts.
 * Centralizes user management logic to avoid duplication between controllers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleJpaRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Validates if a username is available (not already taken)
     *
     * @param username The username to check
     * @return true if the username is available, false if it already exists
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Validates if an email is available (not already taken)
     *
     * @param email The email to check
     * @return true if the email is available, false if it already exists
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Creates a new user account with default API_USER role
     *
     * @param request The registration request with user details
     * @return The created user
     * @throws ConflictException if username or email already exists
     */
    @Transactional
    public User createUser(RegisterRequest request) {
        log.debug("Creating new user: {}", request.getUsername());
        
        // Check if username or email already exists
        if (!isUsernameAvailable(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new ConflictException("Username already exists");
        }
        
        if (!isEmailAvailable(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already exists");
        }
        
        // Validate password strength
        validatePassword(request.getPassword());
        
        // Get default role
        Role userRole = roleRepository.findByName("API_USER")
                .orElseThrow(() -> new RuntimeException("API_USER role not found"));
        
        // Create new user with default role
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>(Set.of(userRole)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully: {}", request.getUsername());
        
        return savedUser;
    }

    /**
     * Creates a new user with specified roles
     *
     * @param request The registration request with user details
     * @param roleNames List of role names to assign to the user
     * @return The created user
     * @throws ConflictException if username or email already exists
     * @throws ResourceNotFoundException if any of the specified roles don't exist
     */
    @Transactional
    public User createUserWithRoles(RegisterRequest request, List<String> roleNames) {
        log.debug("Creating new user with custom roles: {}", request.getUsername());
        
        // Check if username or email already exists
        if (!isUsernameAvailable(request.getUsername())) {
            log.warn("Username already exists: {}", request.getUsername());
            throw new ConflictException("Username already exists");
        }
        
        if (!isEmailAvailable(request.getEmail())) {
            log.warn("Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already exists");
        }
        
        // Validate password strength
        validatePassword(request.getPassword());
        
        // Collect all requested roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> {
                        log.warn("Role not found: {}", roleName);
                        return new ResourceNotFoundException("Role not found: " + roleName);
                    });
            roles.add(role);
        }
        
        // Ensure at least one role is assigned
        if (roles.isEmpty()) {
            log.warn("No roles specified for new user: {}", request.getUsername());
            throw new IllegalArgumentException("User must have at least one role");
        }
        
        // Create new user with specified roles
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .locked(false)
                .roles(roles)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("User created successfully with custom roles: {}", request.getUsername());
        
        return savedUser;
    }

    /**
     * Updates a user's role assignments
     *
     * @param userId The ID of the user to update
     * @param roleNames List of role names to assign to the user
     * @return The updated user
     * @throws ResourceNotFoundException if the user or any specified role doesn't exist
     */
    @Transactional
    public User updateUserRoles(UUID userId, List<String> roleNames) {
        log.debug("Updating roles for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });
        
        // Collect all requested roles
        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> {
                        log.warn("Role not found: {}", roleName);
                        return new ResourceNotFoundException("Role not found: " + roleName);
                    });
            roles.add(role);
        }
        
        // Ensure at least one role is assigned
        if (roles.isEmpty()) {
            log.warn("No roles specified for user update: {}", user.getUsername());
            throw new IllegalArgumentException("User must have at least one role");
        }
        
        // Update user roles
        user.setRoles(roles);
        user.setUpdatedAt(LocalDateTime.now());
        
        User updatedUser = userRepository.save(user);
        log.info("User roles updated successfully for: {}", user.getUsername());
        
        return updatedUser;
    }

    /**
     * Updates a user's enabled/locked status
     *
     * @param userId The ID of the user to update
     * @param enabled Whether the user account should be enabled
     * @param locked Whether the user account should be locked
     * @return The updated user
     * @throws ResourceNotFoundException if the user doesn't exist
     */
    @Transactional
    public User updateUserStatus(UUID userId, Boolean enabled, Boolean locked) {
        log.debug("Updating status for user ID: {}, enabled: {}, locked: {}", 
                userId, enabled, locked);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new ResourceNotFoundException("User not found: " + userId);
                });
        
        // Update status fields if provided
        if (enabled != null) {
            user.setEnabled(enabled);
        }
        
        if (locked != null) {
            user.setLocked(locked);
        }
        
        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);
        log.info("User status updated successfully for: {}", user.getUsername());
        
        return updatedUser;
    }

    /**
     * Retrieves a user by their ID
     *
     * @param userId The ID of the user to retrieve
     * @return The user if found
     * @throws ResourceNotFoundException if the user doesn't exist
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    /**
     * Retrieves a user by their username
     *
     * @param username The username of the user to retrieve
     * @return Optional containing the user if found, empty otherwise
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Retrieves all users in the system
     *
     * @return List of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Deletes a user by their ID
     *
     * @param userId The ID of the user to delete
     * @throws ResourceNotFoundException if the user doesn't exist
     */
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }
        
        userRepository.deleteById(userId);
        log.info("User deleted successfully: {}", userId);
    }

    /**
     * Validates password strength according to security requirements
     * 
     * @param password The password to validate
     * @throws IllegalArgumentException if the password doesn't meet security requirements
     */
    private void validatePassword(String password) {
        // Minimum 8 characters
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        
        // Check for at least one uppercase letter
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        
        // Check for at least one lowercase letter
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        
        // Check for at least one digit
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        
        // Check for at least one special character
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}