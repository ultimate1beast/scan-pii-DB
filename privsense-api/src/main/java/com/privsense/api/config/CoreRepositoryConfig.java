package com.privsense.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for Core repositories.
 * Separates core repository scanning to avoid component scanning conflicts.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.privsense.core.repository",
    entityManagerFactoryRef = "entityManagerFactory"
)
public class CoreRepositoryConfig {
    // Configuration is handled through annotations
}