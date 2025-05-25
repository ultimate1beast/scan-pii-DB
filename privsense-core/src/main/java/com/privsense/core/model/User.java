package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a user in the system.
 * Users can have various roles and permissions.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(exclude = "roles") // Exclude roles from toString to prevent recursion
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash; // Stored as a hash

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Adds a role to the user and updates the inverse relationship
     * @param role The role to add
     */
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }
    
    /**
     * Removes a role from the user and updates the inverse relationship
     * @param role The role to remove
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }
    
    /**
     * Checks if the user has a specific role
     * @param roleName The role name to check
     * @return true if the user has the role, false otherwise
     */
    public boolean hasRole(String roleName) {
        return this.roles.stream()
            .anyMatch(role -> role.getName().equals(roleName));
    }
    
    /**
     * Custom equals method that excludes roles to prevent infinite recursion
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && Objects.equals(id, user.id);
    }

    /**
     * Custom hashCode method that excludes roles to prevent infinite recursion
     */
    @Override
    public int hashCode() {
        // Use a constant value for transient entities (where id is null)
        // Once the entity has an id, use that for the hashCode
        return id != null ? id.hashCode() : 31;
    }
}