package com.privsense.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for Spring Data JPA repositories.
 * Explicitly defines scanning locations to ensure all repositories are properly detected.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
    "com.privsense.api.repository.jpa"
})
public class RepositoryConfig {
    // Configuration is handled through annotations
}