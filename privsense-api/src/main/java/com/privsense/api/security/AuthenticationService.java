package com.privsense.api.security;

import com.privsense.api.repository.jpa.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for authenticating users and generating JWT tokens
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Autowired
    public AuthenticationService(AuthenticationManager authenticationManager,
                                JwtService jwtService,
                                UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Authenticates a user and generates a JWT token
     * 
     * @param username The username
     * @param password The password
     * @return JWT token if authentication is successful
     */
    public String authenticate(String username, String password) {
        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        
        // Get authenticated user details
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        // Update last login time
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        });
        
        // Generate and return JWT token
        return jwtService.generateToken(userDetails);
    }
}