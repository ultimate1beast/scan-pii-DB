package com.privsense.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Data Transfer Object for User information.
 * Contains user details that can be safely returned in API responses.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO extends BaseResponseDTO {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private boolean enabled;
    
    @Builder.Default
    private Set<String> roles = new HashSet<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    
    @JsonIgnore
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        
        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        
        return fullName.length() > 0 ? fullName.toString() : username;
    }
}