package com.privsense.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for returning user details.
 * Contains all relevant user information except sensitive data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}