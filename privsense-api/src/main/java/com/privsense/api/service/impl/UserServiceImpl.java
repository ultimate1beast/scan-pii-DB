package com.privsense.api.service.impl;

import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import com.privsense.core.repository.RoleRepository;
import com.privsense.core.repository.UserRepository;
import com.privsense.core.service.UserService;
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
import java.util.stream.Collectors;

/**
 * Service implementation for managing user accounts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public User registerUser(String username, String email, String password, String firstName, String lastName) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists: " + username);
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        
        // Get the default "USER" role or create if it doesn't exist
        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("USER").description("Regular user").build()));
        
        // Create new user
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .createdAt(LocalDateTime.now())
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>())
                .build();
        
        // Add default role
        user.getRoles().add(userRole);
        
        // Save and return user
        User savedUser = userRepository.save(user);
        log.debug("Registered new user: {}", username);
        
        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public User updateProfile(UUID id, String firstName, String lastName, String email) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        
        // Check if email is being changed and not already in use
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        
        // Update user information
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        
        User updatedUser = userRepository.save(user);
        log.debug("Updated profile for user: {}", updatedUser.getUsername());
        
        return updatedUser;
    }

    @Override
    @Transactional
    public boolean changePassword(UUID id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        
        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            log.warn("Failed password change attempt for user: {}", user.getUsername());
            return false;
        }
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.debug("Password changed for user: {}", user.getUsername());
        return true;
    }

    @Override
    @Transactional
    public User updateRoles(UUID id, Set<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        
        // Find all requested roles
        Set<Role> roles = roleRepository.findByNameIn(roleNames);
        
        // Verify all requested roles exist
        if (roles.size() != roleNames.size()) {
            Set<String> foundRoleNames = roles.stream().map(Role::getName).collect(Collectors.toSet());
            Set<String> missingRoles = new HashSet<>(roleNames);
            missingRoles.removeAll(foundRoleNames);
            
            throw new ResourceNotFoundException("One or more roles not found: " + missingRoles);
        }
        
        // Update user roles
        user.setRoles(roles);
        User updatedUser = userRepository.save(user);
        
        log.debug("Updated roles for user {}: {}", user.getUsername(), roleNames);
        
        return updatedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void updateLastLogin(UUID id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            log.debug("Updated last login for user: {}", user.getUsername());
        });
    }
    
    @Override
    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }
}