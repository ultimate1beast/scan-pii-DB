package com.privsense.api.controller;

import com.privsense.api.dto.AuthRequest;
import com.privsense.api.dto.AuthResponse;
import com.privsense.api.dto.RegisterRequest;
import com.privsense.api.dto.UserDetailsDTO;
import com.privsense.api.exception.ConflictException;
import com.privsense.api.repository.jpa.RoleJpaRepository;
import com.privsense.api.repository.jpa.UserRepository;
import com.privsense.api.security.AuthenticationService;
import com.privsense.api.service.auth.UserService;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Controller handling authentication operations including login and self-registration.
 * These endpoints are publicly accessible and do not require authentication.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
@Tag(name = "Authentication", description = "APIs for user authentication and token management")
public class AuthController {

    private final ObjectProvider<AuthenticationService> authenticationServiceProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectProvider<RoleJpaRepository> roleRepositoryProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Autowired
    public AuthController(ObjectProvider<AuthenticationService> authenticationServiceProvider,
                         ObjectProvider<UserRepository> userRepositoryProvider,
                         ObjectProvider<RoleJpaRepository> roleRepositoryProvider,
                         PasswordEncoder passwordEncoder,
                         UserService userService) {
        this.authenticationServiceProvider = authenticationServiceProvider;
        this.userRepositoryProvider = userRepositoryProvider;
        this.roleRepositoryProvider = roleRepositoryProvider;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    /**
     * Endpoint for authenticating a user
     * This endpoint is publicly accessible and returns a JWT token upon successful authentication.
     * 
     * @param request Authentication request with username and password
     * @return JWT token if authentication is successful
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticates a user and returns a JWT token"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Authentication successful",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.debug("Attempting login for user: {}", request.getUsername());
        String token = authenticationServiceProvider.getObject().authenticate(request.getUsername(), request.getPassword());
        log.info("User logged in successfully: {}", request.getUsername());
        return ResponseEntity.ok(new AuthResponse(token));
    }

    /**
     * Endpoint for self-registration of new users
     * This endpoint is publicly accessible and allows new users to register themselves.
     * New users are assigned the API_USER role by default.
     * 
     * @param request Registration request with user details
     * @return JWT token if registration is successful
     */
    @PostMapping("/register")
    @Transactional
    @Operation(
        summary = "User registration",
        description = "Registers a new user and returns a JWT token"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Registration successful",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Processing self-registration request for username: {}", request.getUsername());
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        RoleJpaRepository roleRepository = roleRepositoryProvider.getObject();
        
        // Check if username or email already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username already exists: {}", request.getUsername());
            throw new ConflictException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email already exists: {}", request.getEmail());
            throw new ConflictException("Email already exists");
        }
        
        // Validate password strength
        validatePassword(request.getPassword());
        
        // Get default user role
        Role userRole = roleRepository.findByName("API_USER")
                .orElseThrow(() -> new RuntimeException("API_USER role not found"));
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
                
        user.addRole(userRole);
        userRepository.save(user);
        
        log.info("User registered successfully: {}", request.getUsername());
        
        // Authenticate the new user
        String token = authenticationServiceProvider.getObject().authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(token));
    }
    
    /**
     * Retrieves the current authenticated user's details
     * This endpoint requires authentication and returns the user details based on the JWT token.
     * 
     * @return The current user's details
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(
        summary = "Get current user details",
        description = "Returns the details of the currently authenticated user",
        security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @ApiResponse(
        responseCode = "200",
        description = "User details retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDetailsDTO.class))
    )
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    public ResponseEntity<UserDetailsDTO> getCurrentUser() {
        // Get the authentication object from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Getting current user details for: {}", authentication.getName());
        
        // Fetch the current user from the repository
        User user = userRepositoryProvider.getObject()
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Map user entity to DTO
        UserDetailsDTO userDetailsDTO = UserDetailsDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
        
        return ResponseEntity.ok(userDetailsDTO);
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