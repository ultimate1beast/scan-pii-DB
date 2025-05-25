package com.privsense.api.config;

import com.privsense.api.service.auth.AuthenticationService;
import com.privsense.api.service.auth.JwtAuthenticationServiceImpl;
import com.privsense.core.config.PrivSenseConfigProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuration for JWT authentication services.
 * This class helps break circular dependencies in the authentication chain.
 */
@Configuration
public class JwtServiceConfig {

    /**
     * Creates the JWT authentication service bean with lazy loading to break circular dependencies.
     * 
     * @param userDetailsServiceProvider Provider for the user details service
     * @param passwordEncoder Service to encode passwords
     * @param configProperties Application configuration properties
     * @return JWT authentication service
     */
    @Bean
    @Lazy
    public AuthenticationService jwtAuthenticationService(
            ObjectProvider<UserDetailsService> userDetailsServiceProvider,
            PasswordEncoder passwordEncoder,
            PrivSenseConfigProperties configProperties) {
        
        return new JwtAuthenticationServiceImpl(
            () -> userDetailsServiceProvider.getObject(),
            passwordEncoder, 
            configProperties);
    }
}