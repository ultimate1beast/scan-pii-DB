package com.privsense.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for authentication requests.
 * Contains credentials for user login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    /**
     * Username for authentication
     */
    @NotBlank(message = "Username is required")
    private String username;
    
    /**
     * Password for authentication
     */
    @NotBlank(message = "Password is required")
    private String password;
}