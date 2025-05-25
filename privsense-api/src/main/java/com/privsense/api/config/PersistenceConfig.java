package com.privsense.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration class for JPA persistence layer.
 * Configures entity scanning and transaction management.
 * Repository scanning is handled at the application level.
 */
@Configuration
@EntityScan(basePackages = {"com.privsense.api.model", "com.privsense.core.model", "com.privsense.db.model"})
@EnableTransactionManagement
public class PersistenceConfig {
    // Configuration is handled by annotations
}