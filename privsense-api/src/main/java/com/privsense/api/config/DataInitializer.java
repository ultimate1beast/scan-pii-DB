package com.privsense.api.config;

import com.privsense.core.repository.RoleRepository;
import com.privsense.core.repository.UserRepository;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Initializes the database with default roles and admin user on startup
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ObjectProvider<UserRepository> userRepositoryProvider;
    private final ObjectProvider<RoleRepository> roleRepositoryProvider;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(ObjectProvider<UserRepository> userRepositoryProvider,
                          ObjectProvider<RoleRepository> roleRepositoryProvider,
                          PasswordEncoder passwordEncoder) {
        this.userRepositoryProvider = userRepositoryProvider;
        this.roleRepositoryProvider = roleRepositoryProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing default roles and users");
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        RoleRepository roleRepository = roleRepositoryProvider.getObject();
        
        // Create default roles if they don't exist
        Role adminRole = createRoleIfNotFound(roleRepository, "ADMIN", "Administrator role with full access");
        createRoleIfNotFound(roleRepository, "API_USER", "Regular API user role");
        
        // Create default admin user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) {
            log.info("Creating default admin user");
            
            User adminUser = User.builder()
                    .username("admin")
                    .email("admin@privsense.com")
                    .passwordHash(passwordEncoder.encode("admin")) // Should be changed after first login
                    .firstName("Admin")
                    .lastName("User")
                    .enabled(true)
                    .locked(false)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            userRepository.save(adminUser);
            log.info("Default admin user created successfully");
        } else {
            log.info("Admin user already exists, skipping creation");
        }
    }
    
    private Role createRoleIfNotFound(RoleRepository roleRepository, String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    log.info("Creating role: {}", name);
                    Role role = Role.builder()
                            .name(name)
                            .description(description)
                            .build();
                    return roleRepository.save(role);
                });
    }
}