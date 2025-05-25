package com.privsense.api.security;

import com.privsense.api.repository.jpa.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Security helper component for user-specific authorization checks.
 * Used in Spring Security's SpEL expressions for method-level security.
 */
@Component("userSecurity")
public class UserSecurityHelper {

    private final UserRepository userRepository;
    
    @Autowired
    public UserSecurityHelper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Checks if the current authenticated user is the same as the requested user ID.
     * Used in @PreAuthorize annotations for self-access checks.
     * 
     * @param userId User ID as a string
     * @return true if current user matches the requested user ID
     */
    public boolean isCurrentUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        String username = authentication.getName();
        
        try {
            UUID id = UUID.fromString(userId);
            return userRepository.findById(id)
                    .map(user -> user.getUsername().equals(username))
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            return false;
        }
    }
}