package com.privsense.api.service.auth;

import com.privsense.api.repository.jpa.UserRepository;
import com.privsense.core.model.Role;
import com.privsense.core.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Service that loads user details from the database for Spring Security authentication.
 * This implementation queries the user repository to authenticate users against database records.
 *
 * @see org.springframework.security.core.userdetails.UserDetailsService
 * @see com.privsense.api.repository.jpa.UserRepository
 */
@Service("userDetailsService")
@Slf4j
public class DatabaseUserDetailsService implements UserDetailsService {

    private final ObjectProvider<UserRepository> userRepositoryProvider;

    @Autowired
    public DatabaseUserDetailsService(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user with username: {}", username);
        
        UserRepository userRepository = userRepositoryProvider.getObject();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        log.debug("User found: {}", user.getUsername());
        
        // Debug user's roles
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            log.warn("User {} has no roles assigned in the database", username);
            
            // Special case for admin user to ensure it always has ADMIN role
            if ("admin".equals(username)) {
                log.info("Adding default ADMIN role for admin user");
                return createAdminUserDetails(user);
            }
        } else {
            log.debug("User {} has roles: {}", username, 
                     user.getRoles().stream()
                         .map(Role::getName)
                         .collect(Collectors.joining(", ")));
        }
        
        return buildUserDetails(user);
    }
    
    private UserDetails buildUserDetails(User user) {
        // Convert the User entity roles to Spring Security authorities
        // Prefix with ROLE_ as Spring Security expects this format
        var authorities = user.getRoles().stream()
                .map(role -> {
                    log.debug("Adding role {} for user {}", role.getName(), user.getUsername());
                    return new SimpleGrantedAuthority("ROLE_" + role.getName());
                })
                .collect(Collectors.toList());
        
        log.debug("Built authorities for {}: {}", user.getUsername(), authorities);
                
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true, // account non-expired
                true, // credentials non-expired
                !user.isLocked(), // account non-locked
                authorities
        );
    }
    
    /**
     * Create a UserDetails object for the admin user with the ADMIN role
     * This is a fallback mechanism to ensure the admin always has admin access
     */
    private UserDetails createAdminUserDetails(User user) {
        Collection<SimpleGrantedAuthority> adminAuthorities = 
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        log.info("Created admin user details with ADMIN role");
        
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                user.isEnabled(),
                true, // account non-expired
                true, // credentials non-expired
                !user.isLocked(), // account non-locked
                adminAuthorities
        );
    }
}