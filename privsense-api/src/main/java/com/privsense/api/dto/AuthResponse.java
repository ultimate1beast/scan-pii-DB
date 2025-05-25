package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for authentication responses
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    
    private String token;
    
    // Can be used for error messages
    private String message;
    
    public AuthResponse(String token) {
        this.token = token;
    }
}