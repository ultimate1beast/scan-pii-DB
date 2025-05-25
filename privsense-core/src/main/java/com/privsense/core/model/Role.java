package com.privsense.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entity representing a role in the system.
 * Roles are used for authorization and permission checking.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@ToString(exclude = "users") // Exclude users from toString to prevent recursion
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.EAGER)
    @Builder.Default
    private Set<User> users = new HashSet<>();
    
    /**
     * Custom equals method that excludes users to prevent infinite recursion
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return id != null && Objects.equals(id, role.id);
    }

    /**
     * Custom hashCode method that excludes users to prevent infinite recursion
     */
    @Override
    public int hashCode() {
        // Use a constant value for transient entities (where id is null)
        // Once the entity has an id, use that for the hashCode
        return id != null ? id.hashCode() : 31;
    }
}