package com.privsense.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Centralized configuration for password encoding.
 * This class isolates the PasswordEncoder bean to break circular dependencies.
 */
@Configuration
public class PasswordEncoderConfig {

    /**
     * Provides a shared instance of BCryptPasswordEncoder for the application.
     * Primary annotation ensures this is the preferred bean when multiple candidates exist.
     * 
     * @return The password encoder instance
     */
    @Bean
    @Primary
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}