package com.privsense.api.dto.auth;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for user role update requests
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleRequest {
    
    @NotEmpty(message = "At least one role must be specified")
    private List<String> roles;
}