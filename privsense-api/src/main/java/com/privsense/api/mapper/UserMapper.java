package com.privsense.api.mapper;

import com.privsense.api.config.MapStructConfig;
import com.privsense.api.dto.UserDTO;
import com.privsense.api.dto.auth.RegisterRequest;
import com.privsense.api.dto.auth.UserResponse;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.AfterMapping;
import org.mapstruct.MappingTarget;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mapper interface for converting between User entities and DTOs.
 * Uses the centralized MapStructConfig configuration.
 */
@Mapper(
    componentModel = "spring",
    config = MapStructConfig.class
)
public interface UserMapper {

    /**
     * Maps User entity to UserDTO.
     * 
     * @param user the User entity
     * @return the UserDTO
     */
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringSet")
    UserDTO toDto(User user);
    
    /**
     * Maps a list of User entities to a list of UserDTOs.
     * 
     * @param users the list of User entities
     * @return list of UserDTOs
     */
    List<UserDTO> toDtoList(List<User> users);
    
    /**
     * Maps User entity to UserResponse for authentication.
     * 
     * @param user the User entity
     * @return the UserResponse
     */
    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToStringList")
    UserResponse toUserResponse(User user);
    
    /**
     * Maps a list of User entities to a list of UserResponses.
     * 
     * @param users the list of User entities
     * @return list of UserResponses
     */
    List<UserResponse> toUserResponseList(List<User> users);
    
    /**
     * Maps RegisterRequest to User entity.
     * Password field requires special handling (encoding) so it's ignored here
     * and should be handled in the service layer.
     * 
     * @param request the RegisterRequest
     * @return User entity (partial, password needs encoding)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true) // Password needs encoding
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "locked", constant = "false")
    @Mapping(target = "roles", ignore = true) // Roles are assigned separately
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "lastLogin", ignore = true)
    User toEntity(RegisterRequest request);
    
    /**
     * Adds metadata after mapping UserDTO
     */
    @AfterMapping
    default void addUserDtoMetadata(@MappingTarget UserDTO dto) {
        dto.addMeta("status", "SUCCESS");
    }
    
    /**
     * Converts a set of Role entities to a set of role names (strings).
     * 
     * @param roles the set of Role entities
     * @return set of role names as strings
     */
    @Named("rolesToStringSet")
    default Set<String> rolesToStringSet(Set<Role> roles) {
        if (roles == null) {
            return new HashSet<>();
        }
        
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
    
    /**
     * Converts a set of Role entities to a list of role names (strings).
     * 
     * @param roles the set of Role entities
     * @return list of role names as strings
     */
    @Named("rolesToStringList")
    default List<String> rolesToStringList(Set<Role> roles) {
        if (roles == null) {
            return new java.util.ArrayList<>();
        }
        
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }
    
    /**
     * Converts UUID to String.
     * 
     * @param id the UUID
     * @return string representation of UUID
     */
    @Named("uuidToString")
    default String uuidToString(java.util.UUID id) {
        return id != null ? id.toString() : null;
    }
}