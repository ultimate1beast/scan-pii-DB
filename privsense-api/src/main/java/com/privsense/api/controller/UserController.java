package com.privsense.api.controller;

import com.privsense.api.dto.auth.RegisterRequest;
import com.privsense.api.dto.auth.RoleRequest;
import com.privsense.api.dto.auth.UserResponse;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.exception.ResourceNotFoundException;
import com.privsense.api.mapper.UserMapper;
import com.privsense.api.repository.jpa.RoleJpaRepository;
import com.privsense.api.repository.jpa.UserRepository;
import com.privsense.api.service.auth.UserService;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Controller for administrative user management operations.
 * All endpoints require ADMIN role except for users getting their own details.
 */
@RestController
@RequestMapping("/api/v1/users")
@Slf4j
@Tag(name = "User Management", description = "APIs for managing user accounts (Admin only)")
public class UserController {

    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectProvider<RoleJpaRepository> roleRepositoryProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserService userService;
    
    @Autowired
    public UserController(
            ObjectProvider<UserRepository> userRepositoryProvider,
            ObjectProvider<RoleJpaRepository> roleRepositoryProvider,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            UserService userService) {
        this.userRepositoryProvider = userRepositoryProvider;
        this.roleRepositoryProvider = roleRepositoryProvider;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    /**
     * Create a new user (Admin only)
     * Allows administrators to create user accounts with any role.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user account with specified details (Admin only)"
    )
    @ApiResponse(
        responseCode = "201", 
        description = "User created successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Admin creating new user: {}", request.getUsername());
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        RoleJpaRepository roleRepository = roleRepositoryProvider.getObject();
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("User creation failed: Username already exists: {}", request.getUsername());
            throw new ConflictException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("User creation failed: Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already exists");
        }
        
        // Validate password strength
        validatePassword(request.getPassword());
        
        // Get default role
        Role userRole = roleRepository.findByName("API_USER")
                .orElseThrow(() -> new RuntimeException("API_USER role not found"));
        
        // Create user with the default role
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
        log.info("Admin created user successfully: {}", request.getUsername());
        
        // Map User entity to UserResponse
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toUserResponse(savedUser));
    }
    
    /**
     * Get all users (Admin only)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "List all users",
        description = "Returns a list of all user accounts in the system (Admin only)"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "User list retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
    )
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        log.debug("Admin retrieving all users");
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        List<User> users = userRepository.findAll();
        
        return ResponseEntity.ok(userMapper.toUserResponseList(users));
    }
    
    /**
     * Get user by ID (Admin or self)
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    @Operation(
        summary = "Get user details",
        description = "Returns details for a specific user (Admin can access any user, regular users can only access their own details)"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "User details retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable("id") String id) {
        log.debug("Retrieving user details for ID: {}", id);
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        return userRepository.findById(UUID.fromString(id))
                .map(user -> {
                    log.debug("User found: {}", user.getUsername());
                    return ResponseEntity.ok(userMapper.toUserResponse(user));
                })
                .orElseGet(() -> {
                    log.warn("User not found for ID: {}", id);
                    throw new ResourceNotFoundException("User not found: " + id);
                });
    }
    
    /**
     * Update user enabled/locked status (Admin only)
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update user status",
        description = "Enables or disables a user account, or locks/unlocks it (Admin only)"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "User status updated successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserResponse> updateUserStatus(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable("id") String id,
            @Parameter(description = "Enable or disable the user account")
            @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "Lock or unlock the user account")
            @RequestParam(required = false) Boolean locked) {
        
        log.info("Admin updating user status for ID: {}, enabled: {}, locked: {}", 
                id, enabled, locked);
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        return userRepository.findById(UUID.fromString(id))
                .map(user -> {
                    if (enabled != null) {
                        user.setEnabled(enabled);
                        log.debug("Setting user {} enabled status to: {}", user.getUsername(), enabled);
                    }
                    if (locked != null) {
                        user.setLocked(locked);
                        log.debug("Setting user {} locked status to: {}", user.getUsername(), locked);
                    }
                    
                    user.setUpdatedAt(LocalDateTime.now());
                    User updatedUser = userRepository.save(user);
                    log.info("User status updated successfully for: {}", user.getUsername());
                    
                    return ResponseEntity.ok(userMapper.toUserResponse(updatedUser));
                })
                .orElseGet(() -> {
                    log.warn("User not found for ID: {}", id);
                    throw new ResourceNotFoundException("User not found: " + id);
                });
    }
    
    /**
     * Update user roles (Admin only)
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update user roles",
        description = "Assigns or removes roles for a user (Admin only)"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "User roles updated successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponse.class))
    )
    @ApiResponse(responseCode = "404", description = "User or role not found")
    public ResponseEntity<UserResponse> updateUserRoles(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable("id") String id,
            @Valid @RequestBody RoleRequest roleRequest) {
        
        log.info("Admin updating roles for user ID: {}", id);
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        RoleJpaRepository roleRepository = roleRepositoryProvider.getObject();
        
        return userRepository.findById(UUID.fromString(id))
                .map(user -> {
                    // Replace existing roles with the new set
                    Set<Role> roles = new HashSet<>();
                    for (String roleName : roleRequest.getRoles()) {
                        Role role = roleRepository.findByName(roleName)
                                .orElseThrow(() -> {
                                    log.warn("Role not found: {}", roleName);
                                    return new ResourceNotFoundException("Role not found: " + roleName);
                                });
                        roles.add(role);
                    }
                    
                    // Ensure the user has at least one role
                    if (roles.isEmpty()) {
                        log.warn("Attempted to set empty roles for user: {}", user.getUsername());
                        throw new IllegalArgumentException("User must have at least one role");
                    }
                    
                    user.setRoles(roles);
                    user.setUpdatedAt(LocalDateTime.now());
                    
                    User updatedUser = userRepository.save(user);
                    log.info("Roles updated successfully for user: {}", user.getUsername());
                    
                    return ResponseEntity.ok(userMapper.toUserResponse(updatedUser));
                })
                .orElseGet(() -> {
                    log.warn("User not found for ID: {}", id);
                    throw new ResourceNotFoundException("User not found: " + id);
                });
    }
    
    /**
     * Delete user (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete user",
        description = "Removes a user account from the system (Admin only)"
    )
    @ApiResponse(responseCode = "204", description = "User deleted successfully")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to delete", required = true)
            @PathVariable("id") String id) {
        
        log.info("Admin deleting user with ID: {}", id);
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        UUID userId = UUID.fromString(id);
        
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            log.info("User deleted successfully: {}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("User not found for deletion: {}", id);
            throw new ResourceNotFoundException("User not found: " + id);
        }
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