package com.privsense.api.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Data Transfer Object for updating user roles.
 * Contains a set of role names to assign to a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    
    @NotEmpty(message = "At least one role must be specified")
    private Set<String> roles;
}