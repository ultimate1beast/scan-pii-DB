package com.privsense.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration class for JPA persistence layer.
 * Configures entity scanning, repository scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(basePackages = {"com.privsense.api.repository", "com.privsense.api.repository.jpa", "com.privsense.core.repository"})
@EntityScan(basePackages = {"com.privsense.api.model", "com.privsense.core.model", "com.privsense.db.model"})
@EnableTransactionManagement
public class PersistenceConfig {
    // Configuration is handled by annotations
}