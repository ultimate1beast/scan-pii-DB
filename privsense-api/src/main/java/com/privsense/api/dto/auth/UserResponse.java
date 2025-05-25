package com.privsense.api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for user information responses
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    
    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private boolean locked;
    private List<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    private String message; // For error messages
    
    public UserResponse(String message) {
        this.message = message;
    }
}