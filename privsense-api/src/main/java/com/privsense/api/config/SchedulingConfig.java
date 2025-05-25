package com.privsense.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks in the application.
 * This allows automated metric persistence and other scheduled operations.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Configuration is handled through annotations
}